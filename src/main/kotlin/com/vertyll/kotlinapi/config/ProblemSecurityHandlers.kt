package com.vertyll.kotlinapi.config

import com.vertyll.kotlinapi.common.exception.Problems
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Answers refusals raised inside the security filter chain — before any controller advice can
 * see them — with the same problem document the rest of the API uses, and with the challenge
 * RFC 6750 asks for: `WWW-Authenticate: Bearer`, with `error="invalid_token"` when a token was
 * sent and refused, and `error="insufficient_scope"` when a valid token lacks the permission.
 */
@Component
class ProblemSecurityHandlers(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint,
    AccessDeniedHandler {
    private val bearerChallenge = BearerTokenAuthenticationEntryPoint()
    private val insufficientScope = BearerTokenAccessDeniedHandler()

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        // Sets the status and the challenge, and leaves the body to us.
        bearerChallenge.commence(request, response, authException)
        write(response, AUTHENTICATION_REQUIRED)
    }

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        insufficientScope.handle(request, response, accessDeniedException)
        write(response, ACCESS_DENIED)
    }

    private fun write(
        response: HttpServletResponse,
        messageKey: String,
    ) {
        val status = HttpStatus.valueOf(response.status)
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        objectMapper.writeValue(response.outputStream, Problems.of(status, messageKey))
    }

    companion object {
        const val AUTHENTICATION_REQUIRED = "errors.auth.authenticationRequired"
        const val ACCESS_DENIED = "errors.auth.accessDenied"
    }
}
