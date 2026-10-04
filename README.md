<p align="center">
    <img alt="" src="https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Keycloak-00b8e3?style=for-the-badge&logo=keycloak&logoColor=4D4D4D">
    <img alt="" src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white">
</p>

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
- Testcontainers.
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

- **Identity provider**: Keycloak (realm `kotlin-api`) owns every page that touches a credential: sign-up, sign-in,
  email verification, password reset, two-factor authentication and acceptance of the terms of use. The application
  never sees a password.
- **Pattern**: BFF. A browser signs in at `GET /api/v1/auth/authorize` with the authorization code flow and PKCE; the
  back-end keeps the tokens and the browser holds only the `KOTLIN_API_SESSION` cookie (`HttpOnly`, `SameSite=Lax`,
  `Secure` in production).
- **Session store**: Redis (Spring Session, `kotlin-api:session` namespace), so the application holds no state of its
  own.
- **JWT**: the API is a stateless OAuth2 resource server verifying signature, issuer, expiry and audience (`kotlin-api`)
  and taking the roles (`USER`, `ADMIN`) from the token; requests with a session get the token attached on the server,
  and a client with its own token calls it with `Authorization: Bearer`.
- **Token lifecycle**: access tokens live five minutes; every refresh returns a new refresh token and invalidates the
  old one, and concurrent requests of one session share a single refresh. Signing out revokes the refresh token at
  Keycloak.
- **Cross-site requests**: `SameSite=Lax` plus `Sec-Fetch-Site`, so a write or a logout sent from another site is
  refused.
- **Accounts**: created in PostgreSQL at the first sign-in, mirroring email, name and roles from Keycloak.

### Core back-end:

- Gradle build system.
- The application has an exception handling mechanism (RFC 9457 problem details with message codes).
- The application has a logging mechanism.
- The application has separate environments for local and prod.
- The application has a dedicated configuration file.
- The application has RBAC (Role Based Access Control).
- The application has Flyway database migration mechanism.
- The application has translations (ICU MessageFormat) editable by an admin.
- And many other features that can be found in the application code.

### Other:

- Docker for development environment.
- Detekt for static code analysis.
- ktlint for code formatting.
