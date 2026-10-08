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

- **Identity provider**: Keycloak (realm `kotlin-api`); the application never sees a password.
- **Pattern**: BFF with Spring Security's OAuth2 client; the browser holds only a session cookie.
- **Session store**: Redis (Spring Session).
- **JWT**: the back-end is a stateless resource server; roles (`USER`, `ADMIN`) come from the token.
- **Details**: [Authentication](./docs/authentication.md).

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
