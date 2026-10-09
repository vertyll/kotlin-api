# Development Setup

## Prerequisites

- Docker or Podman, with Compose
- JDK 25

## Start the infrastructure

```bash
docker compose -f docker-compose.local.yml up -d
```

Every `docker compose` command here works verbatim as `podman compose`.

| Service      | Address                                     | Purpose                                         |
|--------------|---------------------------------------------|-------------------------------------------------|
| PostgreSQL   | `localhost:5432` (`postgres` / `postgres`)  | the `kotlin_api` database                       |
| Redis        | `localhost:6379`                            | sessions and the shared refresh lock            |
| Keycloak     | `http://localhost:9000` (`admin` / `admin`) | realm `kotlin-api`, imported on start           |
| MailDev      | `http://localhost:1080`                     | catches Keycloak's verification and reset mails |
| RedisInsight | `http://localhost:5540`                     | browsing the sessions in Redis                  |

Keycloak imports `keycloak/realm-export.json` on its first start, with two accounts:

| Account                  | Password           | Roles           |
|--------------------------|--------------------|-----------------|
| `admin@kotlin-api.local` | `kotlin-api-local` | `USER`, `ADMIN` |
| `user@kotlin-api.local`  | `kotlin-api-local` | `USER`          |

The realm lives in the `keycloak-data` volume afterwards, so a change to the export file only takes effect after
`docker compose -f docker-compose.local.yml down -v`.

## Run the application

```bash
./gradlew bootRun
```

The `local` profile is the default, and `application-local.yml` already points at the containers above: there is
nothing to configure and no `.env` to create. Flyway migrates the database on start, and the translation catalogue is
filled from `src/main/resources/i18n`.

| Address                                        | What                               |
|------------------------------------------------|------------------------------------|
| `http://localhost:8080/api/v1/swagger-ui.html` | Swagger UI (local profile only)    |
| `http://localhost:8080/api/v1/auth/authorize`  | sign in; returns to the Swagger UI |
| `http://localhost:8080/api/v1/actuator/health` | health                             |

Signing in through the browser leaves a session cookie, so Swagger's "Try it out" calls run as the signed-in user.

## Checks

```bash
./gradlew check -x test   # ktlint and Detekt
./gradlew ktlintFormat    # fix formatting
./gradlew test            # unit and integration tests
```

The integration tests start PostgreSQL and Redis with Testcontainers, so Docker or Podman must be running. CI runs the
same two commands, then the Sonar analysis and the image build.

## Production

The image (`Dockerfile`) runs with the `prod` profile, which takes its settings from the environment:

| Variable                                                                | Purpose                                     |
|-------------------------------------------------------------------------|---------------------------------------------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`                                  | PostgreSQL                                  |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`                            | Redis; TLS unless `REDIS_SSL_ENABLED=false` |
| `KEYCLOAK_REALM_URL`, `KEYCLOAK_CLIENT_SECRET`                          | the realm and the confidential client       |
| `AUTH_CALLBACK_URL`, `AUTH_POST_LOGIN_URL`                              | where Keycloak returns and where it lands   |
| `FRONTEND_URL`                                                          | the only origin CORS admits                 |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP with STARTTLS                          |
