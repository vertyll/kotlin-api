package com.vertyll.kotlinapi.config

import com.vertyll.kotlinapi.common.exception.Problems
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Answers refusals raised inside the security filter chain — before any controller advice can
 * see them — with the same problem document the rest of the API uses.
 */
@Component
class ProblemSecurityHandlers(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint,
    AccessDeniedHandler {
    companion object {
        const val AUTHENTICATION_REQUIRED = "errors.auth.authenticationRequired"
        const val ACCESS_DENIED = "errors.auth.accessDenied"
    }

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) = write(response, HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED)

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) = write(response, HttpStatus.FORBIDDEN, ACCESS_DENIED)

    private fun write(
        response: HttpServletResponse,
        status: HttpStatus,
        messageKey: String,
    ) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        objectMapper.writeValue(response.outputStream, Problems.of(status, messageKey))
    }
}
