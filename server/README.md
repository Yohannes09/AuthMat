# AuthMat Authentication Service

## Authentication Flow
### 1. **User Registration**
- **Endpoint**: `POST /auth/register` *(subject to final routing)*
- **Flow**:
    - User submits `username`, `email`, and `password`.
    - Authentication Service creates the user and returns `201 Created`.
    - Publishes `USER_REGISTERED` event to Kafka.
    - Email Service and others can subscribe to trigger welcome workflows.

### 2. **User Login**
- **Endpoint**: `POST /auth/login` *(subject to final routing)*
- **Flow**:
    - User submits credentials.
    - Authentication Service validates credentials, role, account status (non-locked, not expired, etc.).
    - If valid:
        - Request access and refresh tokens from the **Token Provider**.
        - Returns tokens and user ID.
        - Publishes `USER_LOGGED_IN` event for logging/auditing.

---

## Security

- Services verify JWTs using a shared **public key** (asymmetric signing).
- Tokens carry necessary claims (user ID, roles, expiry, etc.).
- Role-based access control is enforced per service.
- Tokens are issued through **Token Provider**, ensuring a centralized, secure source of truth.

---

## Future Enhancements

- Support for **OAuth 2.0** and third-party identity providers.
- Admin dashboard for system observability and health monitoring.
- HTTPS communication.
