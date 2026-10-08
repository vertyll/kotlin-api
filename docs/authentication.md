# Authentication

- **Identity provider**: Keycloak (realm `kotlin-api`) owns every page that touches a credential: sign-up, sign-in,
  email verification, password reset, two-factor authentication and acceptance of the terms of use. The application
  never sees a password.
- **Pattern**: BFF with Spring Security's OAuth2 client. A browser signs in at `GET /api/v1/auth/authorize` with the
  authorization code flow and PKCE and returns to `/api/v1/auth/callback`; the back-end keeps the tokens and the browser
  holds only the `KOTLIN_API_SESSION` cookie (`HttpOnly`, `SameSite=Lax`, `Secure` in production).
- **Session store**: Redis (Spring Session, `kotlin-api:session` namespace).
- **JWT**: the back-end is a stateless OAuth2 resource server verifying signature, issuer, expiry and audience
  (`kotlin-api`) and taking the roles (`USER`, `ADMIN`) from the token; requests with a session get the token attached
  on the server, and a client with its own token calls it with `Authorization: Bearer`.
- **State**: the back-end is stateless: every request is authorized by the JWT alone, so any instance can serve it. The
  only state is the browser session, and it lives in Redis, outside the application.
- **Token lifecycle**: access tokens live five minutes; every refresh returns a new refresh token and invalidates the
  old one, and concurrent requests of one session share a single refresh, across replicas too (a lock in Redis). Signing
  out revokes the refresh token at Keycloak.
- **Cross-site requests**: `SameSite=Lax` plus `Sec-Fetch-Site`, so a write or a logout sent from another site is
  refused.
- **Accounts**: created in PostgreSQL at the first sign-in, mirroring email, name and roles from Keycloak.
