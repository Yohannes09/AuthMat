
# AuthMat — JWT Filter Architecture & Design Spec

> **Service:** AuthMat (Authentication & Authorization Service)
> **Scope:** JWT validation filter chain, trust model, principal types
> **Stack:** Java · Spring Boot · Spring Security · Redis · AWS KMS
> **Status:** Design phase — pre-implementation
> **Version:** v2 — mTLS replaces HMAC gateway trust token

---

## 1. Overview

AuthMat is the identity provider for the entire ecosystem. Every authenticated request — whether from a human user forwarded by the gateway, or from an internal service like DocKeep — passes through the JWT filter chain before any endpoint logic executes.

This document defines:
- The filter chain and what each filter does
- The mTLS-based trust model and its Kubernetes migration path
- The token signing strategy and how it stays swappable
- The principal types that downstream authorization logic relies on
- The S2S (service-to-service) auth flow

**Design philosophy:**
- Every default is **deny**. Exceptions are explicit and narrow.
- KMS vs internal signing is a **config change, not a code change**.
- Each filter does **exactly one thing**.
- mTLS is enforced at the **application layer now** and migrates to the **mesh layer** on Kubernetes — the application code is designed so that migration is a deletion, not a refactor.
- User identity and service identity are **distinct types** so authorization rules are unambiguous.

---

## 2. Caller types

AuthMat receives requests from two categories of caller. The filter chain handles both, but the identity it builds in the `SecurityContext` differs.

| Caller | Transport trust | Principal built |
|---|---|---|
| Client via gateway | mTLS — gateway presents its certificate | `UserPrincipal` |
| Internal service (DocKeep) | mTLS — service presents its certificate | `ServicePrincipal` |

The filter discriminates between the resulting principal type using a `token_type` claim embedded at JWT issuance:
- User JWT → `"token_type": "user"`
- Service JWT → `"token_type": "service"`

mTLS establishes *transport-layer identity* (this connection came from a trusted peer). The `token_type` claim establishes *application-layer identity* (this request carries a user credential vs a service credential). Both checks run. Neither replaces the other.

---

## 3. Filter chain

Three filters run in sequence before Spring Security's own authorization chain. Each is a `OncePerRequestFilter` bean registered with explicit ordering.

```
Request
  │
  ▼
[1] MtlsEnforcementFilter        ← Did the caller present a trusted certificate?
  │                                 No → 403 immediately
  ▼
[2] JwtAuthenticationFilter      ← Is the token valid?
  │                                 Extract → discriminate (kid) → verify → annotate
  ▼
[3] SecurityContextPopulator     ← Build the right principal type
  │                                 UserPrincipal or ServicePrincipal → SecurityContext
  ▼
Spring Security filter chain     ← @PreAuthorize / hasRole / isUserPrincipal()
  │
  ▼
Response
```

---

## 4. mTLS Trust Infrastructure

### Certificate hierarchy

All internal trust derives from a single private CA you control. No certificates are self-signed at the leaf level — every leaf cert is signed by your CA.

```
ca.key + ca.crt          ← Private CA. ca.key never leaves the machine it was generated on.
       │
       ├── gateway.crt   ← Signed by CA. Presented by the gateway on outbound calls to AuthMat.
       ├── authmat.crt   ← Signed by CA. Presented by AuthMat when services call it.
       └── dockeep.crt   ← Signed by CA. Presented by DocKeep on outbound calls to AuthMat.
```

Each service also gets a `.p12` (PKCS#12) bundle containing its leaf certificate and private key, importable by Spring Boot's TLS configuration.

### What mTLS means in this context

Standard TLS: the client verifies the server's certificate. The server doesn't verify the client.

mTLS: **both sides verify each other's certificate against the shared CA**. When DocKeep calls AuthMat:
1. AuthMat presents `authmat.crt`. DocKeep verifies it was signed by `ca.crt`. ✓
2. DocKeep presents `dockeep.crt`. AuthMat verifies it was signed by `ca.crt`. ✓
3. Connection proceeds. Both parties have cryptographic proof of each other's identity.

An attacker who doesn't hold a certificate signed by your CA cannot complete the TLS handshake at all. The application layer never sees the request.

### Spring Boot server configuration (AuthMat)

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:certs/authmat.p12
    key-store-password: ${AUTHMAT_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    trust-store: classpath:certs/truststore.p12   # contains ca.crt
    trust-store-password: ${AUTHMAT_TRUSTSTORE_PASSWORD}
    client-auth: need                              # REQUIRE client certificate — fail handshake without it
```

`client-auth: need` is the critical line. Spring Boot will reject any TLS connection that does not present a valid client certificate trusted by your CA. This happens at the TLS layer, before any filter runs.

### Spring Boot client configuration (DocKeep calling AuthMat)

```yaml
# DocKeep's RestClient / WebClient bean configuration
authmat:
  mtls:
    key-store: classpath:certs/dockeep.p12
    key-store-password: ${DOCKEEP_KEYSTORE_PASSWORD}
    trust-store: classpath:certs/truststore.p12   # same CA cert
    trust-store-password: ${DOCKEEP_TRUSTSTORE_PASSWORD}
```

Wire this into an `SSLContext` and supply it to the HTTP client factory. The DocKeep service will present its certificate on every outbound call to AuthMat.

### MtlsEnforcementFilter

Even though `client-auth: need` at the TLS layer already blocks uncertified connections, `MtlsEnforcementFilter` is a defense-in-depth check at the application layer. It:

1. Reads the verified peer certificate from the request attribute (`javax.servlet.request.X509Certificate`).
2. Validates that the certificate's Subject DN (Distinguished Name) matches a known caller identity — either `CN=gateway` or `CN=dockeep` depending on the caller type.
3. Validates that the certificate has not expired (independent of the TLS handshake check).
4. If any check fails → `403`.

```java
@Component
@Order(1)
public class MtlsEnforcementFilter extends OncePerRequestFilter {

    private static final Set<String> TRUSTED_CN_VALUES = Set.of("gateway", "dockeep");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        X509Certificate[] certs =
            (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

        if (certs == null || certs.length == 0) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Client certificate required");
            return;
        }

        X509Certificate clientCert = certs[0];

        try {
            clientCert.checkValidity();  // throws CertificateExpiredException or CertificateNotYetValidException
        } catch (CertificateException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Client certificate not valid");
            return;
        }

        String dn = clientCert.getSubjectX500Principal().getName();
        String cn = extractCN(dn);  // parse CN= from RFC 2253 DN string

        if (!TRUSTED_CN_VALUES.contains(cn)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Untrusted client identity: " + cn);
            return;
        }

        // Attach the verified CN to the request for downstream use (e.g. audit logging)
        request.setAttribute("mtls.client.cn", cn);
        chain.doFilter(request, response);
    }
}
```

> **Why this filter still exists when `client-auth: need` already blocks unknown certs?**
>
> `client-auth: need` ensures the cert was signed by your CA. It does not check *which* service that cert belongs to. Without this filter, a cert you issued to DocKeep could be used to call any AuthMat endpoint that DocKeep is not authorized to reach. The CN check enforces service-level access control, not just CA membership.

### Certificate generation

One-time setup. Store `ca.key` offline or in a secrets manager. Never commit it.

```bash
# 1. Generate private CA
openssl req -x509 -newkey rsa:4096 -keyout ca.key -out ca.crt -days 3650 -nodes \
  -subj "/CN=internal-ca/O=AuthDocEcosystem"

# 2. Generate a service keypair and CSR (repeat for each service)
openssl req -newkey rsa:2048 -keyout dockeep.key -out dockeep.csr -nodes \
  -subj "/CN=dockeep/O=AuthDocEcosystem"

# 3. Sign the CSR with your CA
openssl x509 -req -in dockeep.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
  -out dockeep.crt -days 365

# 4. Bundle into PKCS#12 for Spring Boot
openssl pkcs12 -export -in dockeep.crt -inkey dockeep.key -out dockeep.p12 \
  -name dockeep -passout pass:${DOCKEEP_KEYSTORE_PASSWORD}

# 5. Build a truststore containing only ca.crt (shared across all services)
keytool -importcert -file ca.crt -keystore truststore.p12 -storetype PKCS12 \
  -alias internal-ca -storepass ${TRUSTSTORE_PASSWORD} -noprompt
```

Repeat step 2–4 for `authmat` and `gateway`. All services share the same `truststore.p12`.

### Certificate rotation

Leaf certs are issued with a 365-day TTL. Rotation procedure:
1. Generate a new keypair and CSR for the service.
2. Sign with `ca.crt` (no CA rotation needed unless the CA itself is compromised).
3. Deploy the new `.p12` to the service.
4. Old cert expires naturally. No other services need to change because they all trust the CA, not the leaf cert directly.

### Kubernetes migration path

When the system moves to Kubernetes with Istio or Linkerd:
1. The service mesh issues SPIFFE/X.509 SVIDs to each workload automatically.
2. Add an `AuthorizationPolicy` (Istio) enforcing mTLS between services at the mesh layer.
3. Delete `MtlsEnforcementFilter` from the Spring Security config.
4. Remove the `server.ssl.*` block from `application.yml` — TLS termination moves to the sidecar.
5. Nothing else changes. The filter's isolation means the migration is a deletion, not a refactor.

The private CA you operate now maps directly to the concept of a mesh trust domain. The operational knowledge carries over.

> **Network isolation is still the first line of defense.**
> mTLS enforces mutual authentication. Network segmentation ensures services that should never talk to each other cannot even attempt a TLS handshake. In Docker Compose, AuthMat must only be reachable from the gateway and internal services via their shared Docker network, never from the host or external networks.

---

## 5. JwtAuthenticationFilter

**Job:** Extract the token, figure out which verifier to use, verify it, and annotate the response.

### Step 1 — Extract

Read `Authorization: Bearer <token>`. If missing on a protected route → `401`. Public routes (section 8) bypass this filter entirely via `permitAll`.

### Step 2 — Discriminate (without verifying)

Decode the JWT header segment **without verifying the signature**. Read the `kid` (Key ID) claim. The `kid` determines which verifier to invoke:

```
kid = "internal-v1"     →  InternalTokenVerifier
kid = "kms-<key-arn>"   →  KmsTokenVerifier
```

The filter doesn't branch on a config flag — **the token's own metadata drives routing**. This is also what enables key rotation: old tokens with an old `kid` continue to verify until they expire.

### Step 3 — Verify

Delegate to `TokenVerifierStrategy`:

```java
public interface TokenVerifierStrategy {
    Claims verify(String rawToken) throws JwtVerificationException;
}
```

Typed exception → HTTP status mapping:

| Exception | HTTP | Notes |
|---|---|---|
| `ExpiredJwtException` | 401 | Include `WWW-Authenticate: Bearer error="invalid_token"` |
| `SignatureException` | 401 | Don't reveal whether key or token is at fault |
| `MalformedJwtException` | 400 | Token wasn't a valid JWT structure |
| `KeyResolutionException` | 503 | KMS unreachable — infra problem, not auth failure |

> Never catch a generic `Exception` and return 401 — that hides real infrastructure failures.

### Step 4 — Near-expiry annotation

After successful verification, check `exp`. If the token expires within **5 minutes** (configurable), add:

```
X-Token-Expiring-Soon: true
```

The filter does **not** refresh anything. It annotates. DocKeep's HTTP client watches for this header and proactively refreshes before the token expires mid-workflow. This eliminates surprise 401s on long-running operations.

---

## 6. Token signing strategy

Both client tokens and service tokens use the **same** signing mechanism at any given time. Mixing mechanisms across caller types adds complexity with no benefit.

### Active implementations

| Implementation | Environment | How it works |
|---|---|---|
| `InternalTokenVerifier` | Dev / staging | AuthMat signs with a local private key. Public key served at `/auth/.well-known/jwks.json` and cached in Redis. |
| `KmsTokenVerifier` | Production | AWS KMS signs and verifies. No private key material ever touches the application process. JWKS endpoint still works — returns the KMS-backed public key. |

### Switching mechanism

One property controls which implementation is active. Nothing in the filter or downstream code changes:

```yaml
# application.yml
authmat:
  token:
    verifier: internal   # switch to 'kms' for production
```

### Key rotation

The `kid` claim in every JWT header is the rotation mechanism:

1. Issue new tokens with a new key and new `kid`.
2. Old tokens with the old `kid` continue to verify using the old cached key until they expire.
3. Once no valid tokens carry the old `kid`, retire the old key.

Zero-downtime key rotation. `InternalTokenVerifier` handles `kid`-based cache misses by fetching the latest JWKS live from Redis, and falls back to fetching from the JWKS endpoint.

---

## 7. SecurityContextPopulator

**Job:** Read the verified claims, build the right principal type, set it on the `SecurityContext`.

### Shared interface

```java
public interface AuthenticatedPrincipal extends Principal {
    String getId();
    Set<String> getAuthorities();
    boolean isUserPrincipal();
    boolean isServicePrincipal();
}
```

### UserPrincipal

Built for forwarded client requests. Carries user identity and resolved RBAC roles/permissions.

```java
public class UserPrincipal implements AuthenticatedPrincipal {
    String userId;
    String email;
    Set<String> roles;        // e.g. ["ADMIN", "USER"]
    Set<String> permissions;  // e.g. ["doc:read", "doc:write"]

    boolean isUserPrincipal()    { return true; }
    boolean isServicePrincipal() { return false; }
}
```

### ServicePrincipal

Built for S2S calls. Carries service identity and OAuth-style scopes, not user-level roles.

```java
public class ServicePrincipal implements AuthenticatedPrincipal {
    String serviceId;   // e.g. "dockeep"
    String clientId;
    Set<String> scopes; // e.g. ["auth:validate", "auth:introspect"]

    boolean isUserPrincipal()    { return false; }
    boolean isServicePrincipal() { return true; }
}
```

### Why two types instead of one?

With a single flattened `Authentication` type, protecting an admin endpoint from service accounts requires:
```java
// Bad — negative role check, security smell
hasRole('ADMIN') && !hasRole('SERVICE')
```

With distinct types:
```java
// Good — unambiguous, auditable, maps cleanly to RBAC
isUserPrincipal() && hasRole('ADMIN')
isServicePrincipal() && hasScope('auth:validate')
```

The complexity cost is one extra class. The correctness benefit is significant.

---

## 8. Public routes

These routes bypass `MtlsEnforcementFilter` and `JwtAuthenticationFilter` via `permitAll`:

| Route | Why |
|---|---|
| `POST /auth/register` | No token exists yet. User is creating their account. |
| `POST /auth/login` | No token exists yet. User is exchanging credentials for a token. |
| `GET /auth/.well-known/jwks.json` | Public key only — no secrets exposed. Required so DocKeep can bootstrap its own verifier without a chicken-and-egg auth dependency. |
| `GET /actuator/health` | Required by the load balancer. Must be **network-restricted**, not gateway-restricted. All other actuator endpoints stay locked down. |

> **Note on `/auth/token/refresh`:** This is **not** public. It requires a valid refresh token in the `Authorization` header. The client already has that token from login. Refresh tokens are credentials — the endpoint that accepts them must be protected.

> **Note on public routes and mTLS:** `permitAll` bypasses JWT validation, not the TLS layer. Unauthenticated routes like `/auth/login` are still served over TLS — but the server does not require a client certificate for those routes. This is standard TLS, not mTLS, for the routes where the client has no certificate yet (e.g., an end user's browser).

Actuator endpoints belong on a separate management port, never reachable through the gateway:

```yaml
management:
  server:
    port: 8081   # internal only — never exposed through gateway, never externally
  endpoints:
    web:
      exposure:
        include: health, prometheus, info

# main API stays on 8443 (mTLS) and goes through the gateway
```

---

## 9. Service-to-service authentication

With mTLS in place, the certificate **is** the service's identity. The `clientId/clientSecret` client credentials flow is replaced entirely by certificate-based identity.

### How S2S identity works

1. DocKeep presents `dockeep.crt` during the TLS handshake.
2. AuthMat verifies the cert was signed by `ca.crt`.
3. `MtlsEnforcementFilter` extracts `CN=dockeep` from the Subject DN and attaches it to the request.
4. `SecurityContextPopulator` sees `token_type: service` in the JWT and builds a `ServicePrincipal` with `serviceId = "dockeep"`.

The certificate establishes transport trust. The JWT (obtained by DocKeep using its own credentials or issued as part of the service's startup token grant) establishes application-level identity and scopes. Both layers are required.

### Service JWT issuance (startup)

Services still obtain a short-lived JWT to carry in the `Authorization` header of S2S calls. The difference from v1 is that the endpoint issuing this token now requires a valid mTLS client certificate — the cert *is* the authentication credential:

```
POST /auth/service/token
(DocKeep presents dockeep.crt via mTLS — no clientSecret in the body)

Response:
{
  "accessToken": "<jwt>",
  "expiresIn": 300
}
```

The service caches the token and watches for `X-Token-Expiring-Soon` to proactively refresh. TTL recommendation: 5 minutes.

### User context propagation

When DocKeep makes an S2S call to AuthMat (e.g. to validate permissions), it does **not** forward the user's JWT. The user was already validated at the gateway. DocKeep propagates user identity forward via trusted internal headers:

```
X-User-Id:    <userId>
X-User-Roles: ADMIN,USER
```

AuthMat trusts these headers **only from authenticated `ServicePrincipal` callers** (i.e. the `MtlsEnforcementFilter` and `SecurityContextPopulator` must have already run and built a `ServicePrincipal` for the connection). The user identity is established once at the boundary and carried forward as trusted context — not re-validated on every hop. This is the foundation of the zero-trust model.

> **Future — SPIFFE/SPIRE:** When the system moves to Kubernetes with a service mesh, the private CA you operate now is replaced by the mesh's trust domain. Each service gets a SPIFFE SVID automatically. The cert-based identity model you're building now maps directly — the mechanism shifts to infrastructure, the concept stays identical.

---

## 10. Open decisions

| Decision | Current thinking | Notes |
|---|---|---|
| Certificate TTL (leaf certs) | 365 days | Shorter = more operational overhead. Automate rotation before shortening. |
| Service JWT TTL | 5 minutes | Longer TTL = fewer re-auths but wider exposure window on compromise. |
| Near-expiry threshold | 5 minutes | Should be configurable per environment. |
| JWKS cache TTL in Redis | 1 hour | Must be shorter than the key rotation window. Decide rotation window first. |
| `ca.key` storage | Local only for now | Production target: AWS KMS or a hardware security module. Never commit. |
| `client-auth` for public routes | TLS only (no client cert) for `/auth/login`, `/auth/register` | User browsers don't have client certs. mTLS is enforced only on internal service-to-service routes. |
| gRPC migration | N/A yet | When gRPC is introduced for S2S, certs travel via the TLS layer of the gRPC connection — same model, no application changes. |


---

# v1 implementation (deprecated)

> **Service:** AuthMat (Authentication & Authorization Service)
> **Scope:** JWT validation filter chain, trust model, principal types
> **Stack:** Java · Spring Boot · Spring Security · Redis · AWS KMS
> **Status:** Design phase — pre-implementation
 
---

## 1. Overview

AuthMat is the identity provider for the entire ecosystem. Every authenticated request — whether from a human user forwarded by the gateway, or from an internal service like DocKeep — passes through the JWT filter chain before any endpoint logic executes.

This document defines:
- The filter chain and what each filter does
- The gateway trust model and its migration path
- The token signing strategy and how it stays swappable
- The principal types that downstream authorization logic relies on
- The S2S (service-to-service) auth flow

**Design philosophy:**
- Every default is **deny**. Exceptions are explicit and narrow.
- KMS vs internal signing is a **config change, not a code change**.
- Each filter does **exactly one thing**.
- The gateway trust mechanism is designed to be **replaced by mTLS** at the mesh layer without touching application code.
- User identity and service identity are **distinct types** so authorization rules are unambiguous.

---

## 2. Caller types

AuthMat receives requests from two categories of caller. The filter chain handles both, but the identity it builds in the `SecurityContext` differs.

| Caller | Carries | Principal built |
|---|---|---|
| Client via gateway | User JWT (KMS-signed or internally-signed) | `UserPrincipal` |
| Internal service (DocKeep) | Service JWT from client credentials grant | `ServicePrincipal` |

The filter discriminates between them using a `token_type` claim embedded at issuance:
- User JWT → `"token_type": "user"`
- Service JWT → `"token_type": "service"`

---

## 3. Filter chain

Three filters run in sequence before Spring Security's own authorization chain. Each is a `OncePerRequestFilter` bean registered with explicit ordering.

```
Request
  │
  ▼
[1] GatewayTrustFilter          ← Did this come through the gateway?
  │                                No → 403 immediately
  ▼
[2] JwtAuthenticationFilter     ← Is the token valid?
  │                                Extract → discriminate (kid) → verify → annotate
  ▼
[3] SecurityContextPopulator    ← Build the right principal type
  │                                UserPrincipal or ServicePrincipal → SecurityContext
  ▼
Spring Security filter chain    ← @PreAuthorize / hasRole / isUserPrincipal()
  │
  ▼
Response
```
 
---

## 4. GatewayTrustFilter

**Job:** Verify the request came through the gateway. Reject direct calls with `403`.

### How it works (pre-Kubernetes)

The gateway generates a short-lived JWT for every outbound request, signed with a shared HMAC key. AuthMat verifies the signature and checks `exp`. If it's missing or invalid → `403`.

```
Header: X-Internal-Token: <signed-jwt>
 
Claims:
  sub = "gateway"
  iss = "api-gateway"
  iat = <now>
  exp = <now + 30s>
```

**Why this is better than a static shared secret (what DocKeep currently uses):**
- The token expires in 30 seconds — a leaked log entry is useless immediately
- It carries auditable metadata (`sub`, `iss`)
- It's not a secret that lives forever in a config file

### Why not mTLS right now?

mTLS is the correct long-term answer. But mTLS between services belongs at the **infrastructure layer** (service mesh), not application code. Writing certificate management and rotation logic now means writing code that gets entirely discarded when Kubernetes arrives.

> **Kubernetes migration path:**
> 1. Add an `AuthorizationPolicy` rule in Istio/Linkerd enforcing mTLS between the gateway and AuthMat.
> 2. Delete `GatewayTrustFilter` from the Spring Security config.
> 3. Nothing else changes. The filter's isolation means the migration is a deletion, not a refactor.

### Network isolation is the first line of defense

The header check is defense in depth. In Docker Compose right now, AuthMat must only be reachable from the gateway's Docker network. Never rely solely on an application-level header check to enforce perimeter security.
 
---

## 5. JwtAuthenticationFilter

**Job:** Extract the token, figure out which verifier to use, verify it, and annotate the response.

### Step 1 — Extract

Read `Authorization: Bearer <token>`. If missing on a protected route → `401`. Public routes (section 8) bypass this filter entirely via `permitAll`.

### Step 2 — Discriminate (without verifying)

Decode the JWT header segment **without verifying the signature**. Read the `kid` (Key ID) claim. The `kid` determines which verifier to invoke:

```
kid = "internal-v1"     →  InternalTokenVerifier
kid = "kms-<key-arn>"   →  KmsTokenVerifier
```

The filter doesn't branch on a config flag — **the token's own metadata drives routing**. This is also what enables key rotation: old tokens with an old `kid` continue to verify until they expire.

### Step 3 — Verify

Delegate to `TokenVerifierStrategy`:

```java
public interface TokenVerifierStrategy {
    Claims verify(String rawToken) throws JwtVerificationException;
}
```

Typed exception → HTTP status mapping:

| Exception | HTTP | Notes |
|---|---|---|
| `ExpiredJwtException` | 401 | Include `WWW-Authenticate: Bearer error="invalid_token"` |
| `SignatureException` | 401 | Don't reveal whether key or token is at fault |
| `MalformedJwtException` | 400 | Token wasn't a valid JWT structure |
| `KeyResolutionException` | 503 | KMS unreachable — infra problem, not auth failure |

> Never catch a generic `Exception` and return 401 — that hides real infrastructure failures.

### Step 4 — Near-expiry annotation

After successful verification, check `exp`. If the token expires within **5 minutes** (configurable), add:

```
X-Token-Expiring-Soon: true
```

The filter does **not** refresh anything. It annotates. DocKeep's HTTP client watches for this header and proactively refreshes before the token expires mid-workflow. This eliminates surprise 401s on long-running operations.
 
---

## 6. Token signing strategy

Both client tokens and service tokens use the **same** signing mechanism at any given time. Mixing mechanisms across caller types adds complexity with no benefit.

### Active implementations

| Implementation | Environment | How it works |
|---|---|---|
| `InternalTokenVerifier` | Dev / staging | AuthMat signs with a local private key. Public key served at `/auth/.well-known/jwks.json` and cached in Redis. |
| `KmsTokenVerifier` | Production | AWS KMS signs and verifies. No private key material ever touches the application process. JWKS endpoint still works — returns the KMS-backed public key. |

### Switching mechanism

One property controls which implementation is active. Nothing in the filter or downstream code changes:

```yaml
# application.yml
authmat:
  token:
    verifier: internal   # switch to 'kms' for production
```

### Key rotation

The `kid` claim in every JWT header is the rotation mechanism:

1. Issue new tokens with a new key and new `kid`.
2. Old tokens with the old `kid` continue to verify using the old cached key until they expire.
3. Once no valid tokens carry the old `kid`, retire the old key.

Zero-downtime key rotation. `InternalTokenVerifier` handles `kid`-based cache misses by fetching the latest JWKS live from Redis, and falls back to fetching from the JWKS endpoint.
 
---

## 7. SecurityContextPopulator

**Job:** Read the verified claims, build the right principal type, set it on the `SecurityContext`.

### Shared interface

```java
public interface AuthenticatedPrincipal extends Principal {
    String getId();
    Set<String> getAuthorities();
    boolean isUserPrincipal();
    boolean isServicePrincipal();
}
```

### UserPrincipal

Built for forwarded client requests. Carries user identity and resolved RBAC roles/permissions.

```java
public class UserPrincipal implements AuthenticatedPrincipal {
    String userId;
    String email;
    Set<String> roles;        // e.g. ["ADMIN", "USER"]
    Set<String> permissions;  // e.g. ["doc:read", "doc:write"]
 
    boolean isUserPrincipal()    { return true; }
    boolean isServicePrincipal() { return false; }
}
```

### ServicePrincipal

Built for S2S calls. Carries service identity and OAuth-style scopes, not user-level roles.

```java
public class ServicePrincipal implements AuthenticatedPrincipal {
    String serviceId;   // e.g. "dockeep"
    String clientId;
    Set<String> scopes; // e.g. ["auth:validate", "auth:introspect"]
 
    boolean isUserPrincipal()    { return false; }
    boolean isServicePrincipal() { return true; }
}
```

### Why two types instead of one?

With a single flattened `Authentication` type, protecting an admin endpoint from service accounts requires:
```java
// Bad — negative role check, security smell
hasRole('ADMIN') && !hasRole('SERVICE')
```

With distinct types:
```java
// Good — unambiguous, auditable, maps cleanly to RBAC
isUserPrincipal() && hasRole('ADMIN')
isServicePrincipal() && hasScope('auth:validate')
```

The complexity cost is one extra class. The correctness benefit is significant.
 
---

## 8. Public routes

These routes bypass both `GatewayTrustFilter` and `JwtAuthenticationFilter` via `permitAll`:

| Route | Why |
|---|---|
| `POST /auth/register` | No token exists yet. User is creating their account. |
| `POST /auth/login` | No token exists yet. User is exchanging credentials for a token. |
| `GET /auth/.well-known/jwks.json` | Public key only — no secrets exposed. Required so DocKeep can bootstrap its own verifier without a chicken-and-egg auth dependency. |
| `GET /actuator/health` | Required by the load balancer. Must be **network-restricted**, not gateway-restricted. All other actuator endpoints stay locked down. |

> **Note on `/auth/token/refresh`:** This is **not** public. It requires a valid refresh token in the `Authorization` header. The client already has that token from login. Refresh tokens are credentials — the endpoint that accepts them must be protected.

- Prometheus scraping /actuator/prometheus and health check tooling need to be accounted for. The right pattern is to put all actuator endpoints on a **seperate management port**. The port should only be reachable within the internal network, never through the gateway, never externally. 
```yaml
management:
  server:
    port: 8081 # internal only, never exposed through gateway
  endpoints:
    web:
      exposure:
        include: health, prometheus, info

# main API stays on 8080 and goes through the gateway.
```
---

## 9. Service-to-service authentication

Internal services authenticate to AuthMat using the **client credentials flow** (standard OAuth 2.0 machine-to-machine pattern).

### Startup flow (subject to change)

1. Each service has a service account registered in AuthMat with a `clientId` and `clientSecret`.
2. On startup, the service calls `POST /auth/service/token`.
3. AuthMat returns a short-lived service JWT (recommended TTL: 5 minutes).
4. The service caches the token and watches for `X-Token-Expiring-Soon` to proactively refresh.

```
POST /auth/service/token
{
  "clientId": "dockeep",
  "clientSecret": "<secret>"
}
 
Response:
{
  "accessToken": "<jwt>",
  "expiresIn": 300
}
```

### User context propagation

When DocKeep makes an S2S call to AuthMat, it does **not** forward the user's JWT. The user was already validated at the gateway. DocKeep propagates user identity forward via trusted internal headers:

```
X-User-Id:    <userId>
X-User-Roles: ADMIN,USER
```

AuthMat trusts these headers **only from authenticated service principals**. The user identity is established once at the boundary and carried forward as trusted context — not re-validated on every hop. This is the foundation of the zero-trust model.

> **Future — SPIFFE/SPIRE:** When the system moves to Kubernetes with a service mesh, service identity gets formalized using SPIFFE (Secure Production Identity Framework For Everyone). Each service gets a cryptographic SPIFFE ID bound to its workload. The `clientId/clientSecret` pattern used now maps directly to a SPIFFE workload identity — the concept is the same, the mechanism becomes infrastructure instead of application code.
 
---

## 10. Open decisions

These need answers before implementation starts:

| Decision | Current thinking | Notes |
|---|---|---|
| Internal token TTL | 30 seconds | Shorter = better security, more HMAC calls. Confirm latency is acceptable. |
| Service JWT TTL | 5 minutes | Longer TTL = fewer re-auths but wider exposure window on compromise. |
| Near-expiry threshold | 5 minutes | Should be configurable per environment. |
| JWKS cache TTL in Redis | 1 hour | Must be shorter than the key rotation window. Decide rotation window first. |
| gRPC migration | N/A yet | When gRPC is introduced for S2S, tokens travel as gRPC metadata instead of HTTP headers. Filter logic is unchanged. |

---

# Pushback

## 4. Shared HMAC Key
- Acceptable for current stage
- Pragmatic intermediate step, before mTLS

## 
