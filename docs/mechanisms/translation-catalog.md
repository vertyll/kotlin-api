# Translation catalog

Where the text behind every message key comes from, and how an administrator's edits survive a deployment.

The catalog ships in `src/main/resources/i18n/en.json` and `pl.json`, ICU MessageFormat. At startup
`TranslationSynchronizer` brings the `translation` table in line with those files: new keys are added, changed defaults
adopted and keys the code no longer uses dropped.

An admin can override any message. An override survives a new default until it is reset, and it is checked before it
is stored: it must parse as ICU MessageFormat and may use only the placeholders of its default, so a typo cannot break
the message at runtime.
