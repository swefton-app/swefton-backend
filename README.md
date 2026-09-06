# Swefton Backend

Requires Java 25 and the Infisical CLI for local development.

Authenticate with `infisical login`, then run `infisical init` in this directory
and select the **Swefton Backend** project. Configure these keys in the `dev`
environment at path `/`:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
MAIL_FROM
REDIS_HOST
REDIS_PORT
CORS_ALLOWED_ORIGINS
STORAGE_PROVIDER
FILE_UPLOAD_DIR
MEDIA_VIDEO_MAX_DURATION_SECONDS
SERVER_PORT
```

Start from PowerShell:

```powershell
infisical run --env=dev --path=/ -- .\mvnw.cmd spring-boot:run
```

[Infisical run](https://infisical.com/docs/cli/commands/run) injects secrets into
the application process environment. Spring Boot resolves `${KEY}` placeholders
from its environment; this project does not load or export a `.env` file.
The application configuration supplies no fallback values for these keys.

The `app.*` and `media.*` settings are declared for future consumers; the current
starter has no corresponding beans. Unused placeholders alone do not validate
missing values at startup. Springdoc settings also require a Springdoc dependency
before Swagger endpoints are available.

Run the existing context test with isolated, non-secret test values and an
in-memory H2 database:

```powershell
.\mvnw.cmd test
```
