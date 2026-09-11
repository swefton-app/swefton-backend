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
MAIL_SMTP_STARTTLS_ENABLE
MAIL_SMTP_STARTTLS_REQUIRED
MAIL_SMTP_SSL_ENABLE
REDIS_HOST
REDIS_PORT
CORS_ALLOWED_ORIGINS
GOOGLE_CLIENT_ID
STORAGE_PROVIDER
FILE_UPLOAD_DIR
MEDIA_VIDEO_MAX_DURATION_SECONDS
SERVER_PORT
JWT_SECRET
JWT_ACCESS_TOKEN_SECONDS
REFRESH_TOKEN_SECONDS
```

`JWT_SECRET` must be a Base64-encoded value containing at least 32 decoded
bytes. `JWT_ISSUER` is optional and defaults to `swefton`.

SMTP connectivity is not checked during startup by default. Set the optional
`MAIL_TEST_CONNECTION=true` when the application should fail fast if the mail
server cannot be reached.

The development Mailtrap configuration uses port `465` with implicit SSL. For
STARTTLS providers, use their STARTTLS port and set `MAIL_SMTP_SSL_ENABLE=false`
and `MAIL_SMTP_STARTTLS_ENABLE=true`.

Email verification codes expire after 600 seconds, resend requests have a
60-second cooldown, and users receive 5 verification attempts by default.
Override these values with `EMAIL_VERIFICATION_CODE_TTL_SECONDS`,
`EMAIL_VERIFICATION_RESEND_COOLDOWN_SECONDS`, and
`EMAIL_VERIFICATION_MAX_ATTEMPTS`, respectively.

The temporary `app.seed` block in `application.yml` seeds all four roles and
the `admin@swefton.local`, `trainer@swefton.local`, and `user@swefton.local`
development accounts. Remove the block and seeder after the first successful
startup. The seed is idempotent, so existing roles and email addresses are not
duplicated.

Start from PowerShell:

```powershell
infisical run --env=dev --path=/ -- .\mvnw.cmd spring-boot:run
```

[Infisical run](https://infisical.com/docs/cli/commands/run) injects secrets into
the application process environment. Spring Boot resolves `${KEY}` placeholders
from its environment; this project does not load or export a `.env` file.
The application configuration supplies no fallback values for these keys.

The storage, email, security, and media settings are consumed by application
components and must be available when the corresponding components start.

Run the existing context test with isolated, non-secret test values and an
in-memory H2 database:

```powershell
.\mvnw.cmd test
```
