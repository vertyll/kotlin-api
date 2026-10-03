## Project Assumptions

Showcase Kotlin and Spring Boot API.

## Technology Stack

### Back-end:

- Spring Boot.
- Kotlin.
- Gradle Kotlin DSL.
- JDBC.
- JPA.
- Hibernate.
- PostgreSQL.
- JUnit.
- Mockito.
- Spring Security.
- Spring Data.
- Spring Web.
- Spring Mail.
- Spring Session (Redis).
- Keycloak.
- Flyway.
- OpenAPI (Swagger).
- ICU4J.

### Authentication:

- Keycloak (realm `kotlin-api`) handles sign-up, sign-in, email verification, password reset, two-factor
  authentication and acceptance of the terms of use on its own pages.
- A browser signs in at `GET /api/v1/auth/authorize` (`?register=true` opens sign-up): the authorization code flow
  with PKCE, after which the browser holds only the `KOTLIN_API_SESSION` cookie (`HttpOnly`, `SameSite=Lax`, `Secure` in
  production). The tokens stay in the session, kept in Redis, so the application holds no state of its own;
  `GET /api/v1/auth/session` says who is signed in and `POST /api/v1/auth/logout` ends the session here and at Keycloak.
- A request with a session gets its access token attached on the server, refreshed when it is about to expire;
  refresh tokens rotate on every use and concurrent requests share a single refresh. A cross-site write is not given
  the token.
- The API is a stateless OAuth2 resource server: it verifies the token's signature, issuer, expiry and audience
  (`kotlin-api`) and takes the realm roles (`USER`, `ADMIN`) from it. A client with its own Keycloak token calls it
  with `Authorization: Bearer`.
- The account is created at the first sign-in and mirrors the email, name and roles from Keycloak on every sign-in and
  on `GET /api/v1/users/me`.
- The terms and the privacy policy are served at `/api/v1/legal/terms.html` and `/api/v1/legal/privacy.html`.

### Core back-end:

- Endpoints return plain data; every error is an RFC 9457 document (`application/problem+json`) whose `code`
  names a message key, with ICU arguments in `args` and rejected fields in `errors` (a validation problem keys
  `args` by field too).
- Messages live in an ICU MessageFormat catalogue: defaults ship in `src/main/resources/i18n/{pl,en}.json` and are
  synchronized into the database at startup. Clients fetch `GET /api/v1/translations/{pl|en}`; an admin lists,
  overrides and resets messages under `/api/v1/admin/translations` (an override must parse and may use only the
  arguments of its default).
- The application has logging mechanism.
- The application has email configuration (`spring.mail.*`, `application.mail.from`) for its own messages.
- The application has separate environments for dev and prod.
- The application has separate configuration file.
- The application has RBAC (Role Based Access Control).
- The application has Flyway database migration mechanism.
- And many other features that can be found in the application code.

### Other:

- Detekt for static code analysis.
- ktlint for static code analysis and maintaining consistent code quality.
- Docker for development environment (`docker compose -f docker-compose.local.yml up -d`: PostgreSQL :5432,
  Redis :6379, Keycloak :9000 with the realm from `keycloak/realm-export.json`, maildev :1025/:1080); the local realm
  has `admin@kotlin-api.local` (`ADMIN`) and `user@kotlin-api.local`, both with the password `kotlin-api-local`.
  Tests start their own PostgreSQL and Redis with Testcontainers.
