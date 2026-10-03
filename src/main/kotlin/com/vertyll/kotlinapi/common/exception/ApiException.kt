package com.vertyll.kotlinapi.common.exception

import org.springframework.http.HttpStatus

class ApiException(
    val messageKey: String,
    val status: HttpStatus,
    val args: Map<String, Any> = emptyMap(),
    cause: Throwable? = null,
) : RuntimeException(messageKey, cause)
