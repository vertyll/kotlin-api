package com.vertyll.kotlinapi.common.exception

import jakarta.validation.ConstraintViolation
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.validation.FieldError
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    companion object {
        const val VALIDATION_FAILED = "errors.common.validationFailed"
        const val BAD_REQUEST = "errors.common.badRequest"
        const val UNEXPECTED = "errors.common.unexpected"
        const val INVALID_VALUE = "validation.invalid"
        const val INVALID_CREDENTIALS = "errors.auth.invalidCredentials"
        const val ACCOUNT_DISABLED = "errors.auth.accountDisabled"
        const val ACCOUNT_LOCKED = "errors.auth.accountLocked"
        const val ACCESS_DENIED = "errors.auth.accessDenied"
        private val CONSTRAINT_METADATA = setOf("message", "groups", "payload")
    }

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ProblemDetail = Problems.of(ex.status, ex.messageKey, ex.args)

    /**
     * Lists each rejected field's keys; a constraint's own attributes (`min`, `max`, …) become that
     * field's ICU arguments, keyed by field since two fields may share a rule with different limits.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ProblemDetail {
        val errors = linkedMapOf<String, MutableList<String>>()
        val args = linkedMapOf<String, MutableMap<String, Any>>()
        ex.bindingResult.fieldErrors.forEach { error ->
            errors.getOrPut(error.field) { mutableListOf() }.add(error.defaultMessage ?: INVALID_VALUE)
            val attributes = constraintArguments(error)
            if (attributes.isNotEmpty()) {
                args.getOrPut(error.field) { linkedMapOf() }.putAll(attributes)
            }
        }
        return Problems.of(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, args).apply {
            setProperty(Problems.ERRORS_PROPERTY, errors)
        }
    }

    private fun constraintArguments(error: FieldError): Map<String, Any> =
        if (error.contains(ConstraintViolation::class.java)) {
            error
                .unwrap(ConstraintViolation::class.java)
                .constraintDescriptor.attributes
                .filterKeys { it !in CONSTRAINT_METADATA }
        } else {
            emptyMap()
        }

    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class)
    fun handleUnreadableRequest(ex: Exception): ProblemDetail {
        log.debug("Unreadable request: {}", ex.message)
        return Problems.of(HttpStatus.BAD_REQUEST, BAD_REQUEST)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(): ProblemDetail = Problems.of(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS)

    @ExceptionHandler(DisabledException::class)
    fun handleDisabledException(): ProblemDetail = Problems.of(HttpStatus.FORBIDDEN, ACCOUNT_DISABLED)

    @ExceptionHandler(LockedException::class)
    fun handleLockedException(): ProblemDetail = Problems.of(HttpStatus.FORBIDDEN, ACCOUNT_LOCKED)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(): ProblemDetail = Problems.of(HttpStatus.FORBIDDEN, ACCESS_DENIED)

    /**
     * Spring's own failures — an unknown path, an unsupported method — already carry the status
     * they deserve and a document of their own, so they pass through untouched; only what nothing
     * recognized becomes a `500`.
     */
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ProblemDetail {
        if (ex is ErrorResponse) {
            return ex.body
        }
        log.error("Unhandled exception", ex)
        return Problems.of(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED)
    }
}
