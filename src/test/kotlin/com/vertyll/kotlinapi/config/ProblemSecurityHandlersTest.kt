package com.vertyll.kotlinapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.json.ProblemDetailJacksonMixin
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

class ProblemSecurityHandlersTest {
    // As Spring's own converters render a problem, with its properties at the top level.
    private val objectMapper = JsonMapper.builder().addMixIn(ProblemDetail::class.java, ProblemDetailJacksonMixin::class.java).build()
    private val handlers = ProblemSecurityHandlers(objectMapper)
    private val response = MockHttpServletResponse()

    @Test
    fun aRequestWithoutATokenIsChallengedForOne() {
        handlers.commence(MockHttpServletRequest(), response, InsufficientAuthenticationException("Full authentication is required"))

        assertThat(response.status).isEqualTo(401)
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer ").doesNotContain("error=")
        assertProblem(401, ProblemSecurityHandlers.AUTHENTICATION_REQUIRED)
    }

    @Test
    fun aRefusedTokenIsNamedInvalidInTheChallenge() {
        handlers.commence(MockHttpServletRequest(), response, InvalidBearerTokenException("Jwt expired"))

        assertThat(response.status).isEqualTo(401)
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer error=\"invalid_token\"")
        assertProblem(401, ProblemSecurityHandlers.AUTHENTICATION_REQUIRED)
    }

    @Test
    fun aValidTokenWithoutThePermissionIsNamedInsufficientInTheChallenge() {
        val request = MockHttpServletRequest().apply { userPrincipal = JwtAuthenticationToken(token()) }

        handlers.handle(request, response, AccessDeniedException("Access Denied"))

        assertThat(response.status).isEqualTo(403)
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer error=\"insufficient_scope\"")
        assertProblem(403, ProblemSecurityHandlers.ACCESS_DENIED)
    }

    private fun assertProblem(
        status: Int,
        code: String,
    ) {
        assertThat(response.contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        val problem = objectMapper.readTree(response.contentAsByteArray)
        assertThat(problem.get("status").asInt()).isEqualTo(status)
        assertThat(problem.get("code").asString()).isEqualTo(code)
    }

    private fun token(): Jwt =
        Jwt
            .withTokenValue("token")
            .header("alg", "RS256")
            .subject("user")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build()
}
