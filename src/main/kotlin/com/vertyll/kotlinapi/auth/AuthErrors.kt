package com.vertyll.kotlinapi.auth

import com.vertyll.kotlinapi.common.exception.ApiException
import org.springframework.http.HttpStatus

object AuthErrors {
    const val SIGN_IN_REJECTED = "errors.auth.signInRejected"
    const val SESSION_EXPIRED = "errors.auth.sessionExpired"
    const val IDENTITY_PROVIDER_UNAVAILABLE = "errors.auth.identityProviderUnavailable"

    fun rejected(
        messageKey: String,
        cause: Throwable? = null,
    ): ApiException = ApiException(messageKey, HttpStatus.UNAUTHORIZED, cause = cause)

    fun unavailable(cause: Throwable? = null): ApiException =
        ApiException(IDENTITY_PROVIDER_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, cause = cause)
}
