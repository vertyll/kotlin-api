# Cross-site requests

Why a forged request from another site cannot act with the user's session.

A browser sends the session cookie with every request to this host, whichever page made it, so a write that the
session authenticates also needs a CSRF token, which a page of another site cannot know. Two older defenses stay in
place behind it.

## The CSRF token

Spring Security's CSRF protection is on, with the token kept in a cookie (`CookieCsrfTokenRepository`) and read the way
a single-page application reads it (`csrf.spa()`):

- every response carries the `__Host-XSRF-TOKEN` cookie, `Path=/`, `SameSite=Lax`, `Secure` and without `Domain`; it is
  not `HttpOnly`, since a script of this origin must read it, and it holds no credential. The `__Host-` prefix makes the
  browser accept it only from this host, so a sibling host under the same parent domain cannot plant a token of its
  own. The cookie is `Secure`, and so prefixed, exactly when the session cookie is: the local profile, served over
  plain HTTP, names it `XSRF-TOKEN`, as the prefix requires `Secure`;
- a `POST`, `PUT`, `PATCH` or `DELETE` sends the cookie's value back in the `X-XSRF-TOKEN` header; without it, or with
  a value that does not match, the request is refused with `403` and the `errors.auth.accessDenied` problem document
  before it reaches a controller;
- the token is replaced at sign-in, so a token known before it is worthless after it.

A page of another site can make the browser send the cookie, but cannot read it, so it cannot write the header. The API
has no front-end of its own; its one browser client is the Swagger UI of the local profile, which sends the header
itself (`springdoc.swagger-ui.csrf.enabled`), reading the cookie named by `springdoc.swagger-ui.csrf.cookie-name`,
which each profile sets to the name the filter chain gives it. A front-end served from this origin reads the cookie and
sends the header the same way.

## Requests with a bearer token need none

A client that calls the API with its own access token, `Authorization: Bearer`, has no session to forge, and the token
is not asked of it. `BearerTokenRequests` finds the token as the resource server does (`DefaultBearerTokenResolver`), so
exactly the requests the token authenticates are exempt: a request with another scheme, such as `Basic`, is still
authenticated by its session cookie and still needs the CSRF token, and a malformed bearer token is exempt only to be
refused with `401`. A browser never adds an `Authorization` header by itself, and another site's page cannot set one
without a CORS preflight, which `CorsConfig` allows only for `application.frontend.url`.

## Defense in depth

- The session cookie is `SameSite=Lax`, which keeps it off cross-site `POST`, `PUT`, `PATCH` and `DELETE`.
- Lax still treats another subdomain of the same site as same-site. Such a subdomain cannot plant the `__Host-` CSRF
  cookie, but the bare cookie of the local profile has no such guard, and `SessionAccessTokenFilter` also reads
  `Sec-Fetch-Site` (`FetchMetadata`): an unsafe method sent from anywhere other than this origin gets no token from the
  session and is answered `401`, whatever CSRF token it carries. `POST /auth/logout` applies the same rule and answers
  `403`, so another page cannot sign the person out.
