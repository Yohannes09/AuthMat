# AuthMat — Identity and Access Management

## Key Features

- **Secure Authentication and Authorization**
  - Supports user registration, login, refresh, and credential updates.
  - Issues JWT access token and Opaque refresh token, with Redis-backed validations for performance.

- **Token-Based Access Control**
  - Stateless token flows for authentication.

- **Admin Management Tools**
  - Admin capabilities for user moderation, credential resets, and enforcing account restrictions like bans or access revocation.

- **Optimized for Performance and Scalability**
  - Redis for sub-100ms user status lookups (e.g. roles, permissions, account state).
  - Java Virtual Threads enable high-concurrency, non-blocking request handling.
  - PostgreSQL persistence with tuned indexes for fast querying.