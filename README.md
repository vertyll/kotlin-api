## Project Assumptions
Showcase Kotlin and Spring Boot API.

## Technology Stack

### Back-end:
- Spring Boot
- Kotlin
- Gradle Kotlin DSL
- JDBC
- JPA
- Hibernate
- PostgreSQL
- JUnit
- Mockito
- Spring Security
- Spring Data
- Spring Web
- Spring Mail
- Flyway
- OpenAPI (Swagger)

### Authentication:
- JWT-based authentication – the application uses JWT tokens for user authentication and includes token refresh mechanism (http only secure cookie)
- the application allows logging in on multiple devices simultaneously

### Core back-end:
- the application has exception handling mechanism
- the application has logging mechanism
- the application has email sending mechanism
- the application has scheduled task handling mechanism (cron)
- the application has separate environments for dev and prod
- the application has separate configuration file
- the application has RBAC (Role Based Access Control)
- the application has Flyway database migration mechanism
- and many other features that can be found in the application code

### Other:
- Detekt for static code analysis
- ktlint for static code analysis and maintaining consistent code quality
- Docker for development environment

> [!NOTE]
> During application development, SOLID principles, DRY, composition over inheritance, dependency injection, design patterns, architectural patterns were applied, tests were written, and other good programming practices were adopted.
