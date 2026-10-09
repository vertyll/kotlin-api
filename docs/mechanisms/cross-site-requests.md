# Cross-site requests

Why a forged request from another site cannot act with the user's session, without CSRF tokens.

The cookie is `SameSite=Lax`, which keeps it off cross-site `POST`, `PUT`, `PATCH` and `DELETE`. `FetchMetadata` adds a
second check: an unsafe request whose `Sec-Fetch-Site` is neither `same-origin` nor `none` gets no token, and a logout
from another site is refused with `403`. CSRF tokens are therefore not used.
