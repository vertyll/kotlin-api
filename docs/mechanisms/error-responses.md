# Error responses

What the API answers when it refuses a request, and how a client turns that into text.

Every refusal is an RFC 9457 problem document (`application/problem+json`, built by `Problems` and
`GlobalExceptionHandler`). The server never sends prose a person reads:

| Field    | Holds                                                                 |
|----------|-----------------------------------------------------------------------|
| `status` | the HTTP status                                                       |
| `detail` | the same key as `code`                                                |
| `code`   | a key of the translation catalog, e.g. `errors.user.notFound`         |
| `args`   | the ICU arguments for that key; in a validation error, keyed by field |
| `errors` | in a validation error, the message keys of each invalid field         |

The client translates: it loads the catalog once from `GET /translations/{language}` and formats `code` with `args`
as an ICU MessageFormat message, in its reader's language. A new error is therefore a new key in
`src/main/resources/i18n/en.json` and `pl.json`, never a sentence in the code.
