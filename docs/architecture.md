# Architecture

## Packages

The code is split by feature, and each feature keeps its own controller, service, repository and model.

| Package       | Holds                                                                           |
|---------------|---------------------------------------------------------------------------------|
| `auth`        | sign-in, the session, token refresh and the identity read from Keycloak's token |
| `user`        | the local account mirrored from Keycloak                                        |
| `role`        | the roles the application knows (`USER`, `ADMIN`)                               |
| `translation` | the message catalog, its defaults and the admin endpoints that override them    |
| `config`      | security, CORS, OpenAPI and auditing                                            |
| `common`      | the base entity and the error handling                                          |

## Endpoints

The API is described by OpenAPI: the Swagger UI at `http://localhost:8080/api/v1/swagger-ui.html` (local profile only)
lists every endpoint with its request and response.

Access is decided in two places: `SecurityConfig` admits the public paths and requires a signed-in user for the rest,
and `@PreAuthorize("hasRole('ADMIN')")` guards the admin endpoints.

## Accounts mirror Keycloak

Keycloak owns the person: credentials, email, name and realm roles. The `user` table holds a copy keyed by the Keycloak
identifier, written by `UserService.sync` at every sign-in. Roles the application does not know are ignored, so a role
added in Keycloak for another purpose never reaches the database.

## Errors are message keys

Every refusal is an RFC 9457 problem document carrying a message key instead of a sentence: [Error
responses](mechanisms/error-responses.md).

## Translations

Every message is a key of a catalog an administrator can edit: [Translation catalog](mechanisms/translation-catalog.md).

## Database

PostgreSQL, migrated by Flyway (`src/main/resources/db/migration`); Hibernate only validates the schema
(`ddl-auto: validate`). Entities extend `BaseEntity`, whose JPA auditing records when and by whom a row was created and
last changed; a change made without a signed-in user is recorded as `SYSTEM`.
