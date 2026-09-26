package com.vertyll.kotlinapi.common.exception

import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail

/**
 * Builds the RFC 9457 document every refusal is answered with.
 *
 * `detail` and `code` carry a key of the translation catalogue rather than prose, and `args`
 * the ICU arguments for it; the client renders the message in its reader's language.
 */
object Problems {
    const val CODE_PROPERTY = "code"
    const val ARGS_PROPERTY = "args"
    const val ERRORS_PROPERTY = "errors"

    fun of(
        status: HttpStatusCode,
        messageKey: String,
        args: Map<String, Any> = emptyMap(),
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, messageKey).apply {
            setProperty(CODE_PROPERTY, messageKey)
            if (args.isNotEmpty()) {
                setProperty(ARGS_PROPERTY, args)
            }
        }
}
