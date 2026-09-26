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
- Flyway.
- OpenAPI (Swagger).
- ICU4J.

### Authentication:

- JWT-based authentication – the application uses JWT tokens for user authentication
and includes token refresh mechanism (http only secure cookie).
- The application allows logging in on multiple devices simultaneously.

### Core back-end:

- Endpoints return plain data; every error is an RFC 9457 document (`application/problem+json`) whose `code`
  names a message key, with ICU arguments in `args` and rejected fields in `errors` (a validation problem keys
  `args` by field too).
- Messages live in an ICU MessageFormat catalogue: defaults ship in `src/main/resources/i18n/{pl,en}.json` and are
  synchronized into the database at startup. Clients fetch `GET /api/v1/translations/{pl|en}`; an admin lists,
  overrides and resets messages under `/api/v1/admin/translations` (an override must parse and may use only the
  arguments of its default).
- The application has logging mechanism.
- The application has email sending mechanism.
- The application has scheduled task handling mechanism (cron).
- The application has separate environments for dev and prod.
- The application has separate configuration file.
- The application has RBAC (Role Based Access Control).
- The application has Flyway database migration mechanism.
- And many other features that can be found in the application code.

### Other:

- Detekt for static code analysis.
- ktlint for static code analysis and maintaining consistent code quality.
- Docker for development environment (`docker compose -f docker-compose.dev.yml up -d`: PostgreSQL and maildev);
  tests start their own PostgreSQL with Testcontainers.
