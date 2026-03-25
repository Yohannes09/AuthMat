# AuthMat — JWT Filter Architecture & Design Spec

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
