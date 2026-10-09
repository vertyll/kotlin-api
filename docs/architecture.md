# Architecture

## Packages

The code is split by feature, and each feature keeps its own controller, service, repository and model.

| Package       | Holds                                                                           |
|---------------|---------------------------------------------------------------------------------|
| `auth`        | sign-in, the session, token refresh and the identity read from Keycloak's token |
| `user`        | the local account mirrored from Keycloak                                        |
| `role`        | the roles the application knows (`USER`, `ADMIN`)                               |
| `translation` | the message catalogue, its defaults and the admin endpoints that override them  |
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

Every refusal is an RFC 9457 problem document (`application/problem+json`, built by `Problems` and
`GlobalExceptionHandler`). The server never sends prose a person reads:

| Field    | Holds                                                                 |
|----------|-----------------------------------------------------------------------|
| `status` | the HTTP status                                                       |
| `detail` | the same key as `code`                                                |
| `code`   | a key of the translation catalogue, e.g. `errors.user.notFound`       |
| `args`   | the ICU arguments for that key; in a validation error, keyed by field |
| `errors` | in a validation error, the message keys of each invalid field         |

The client translates: it loads the catalogue once from `GET /translations/{language}` and formats `code` with `args`
as an ICU MessageFormat message, in its reader's language. A new error is therefore a new key in
`src/main/resources/i18n/en.json` and `pl.json`, never a sentence in the code.

## Translations

The catalogue ships in `src/main/resources/i18n/en.json` and `pl.json`, ICU MessageFormat. At startup
`TranslationSynchronizer` brings the `translation` table in line with those files: new keys are added, changed defaults
adopted and keys the code no longer uses dropped.

An admin can override any message. An override survives a new default until it is reset, and it is checked before it
is stored: it must parse as ICU MessageFormat and may use only the placeholders of its default, so a typo cannot break
the message at runtime.

## Database

PostgreSQL, migrated by Flyway (`src/main/resources/db/migration`); Hibernate only validates the schema
(`ddl-auto: validate`). Entities extend `BaseEntity`, whose JPA auditing records when and by whom a row was created and
last changed; a change made without a signed-in user is recorded as `SYSTEM`.
