package com.vertyll.kotlinapi.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest

class BearerTokenRequestsTest {
    private val bearerTokenRequests = BearerTokenRequests()

    @Test
    fun `a request with a bearer token matches`() {
        assertThat(bearerTokenRequests.matches(requestWithAuthorization("Bearer eyJhbGciOiJSUzI1NiJ9"))).isTrue()
    }

    @Test
    fun `the scheme matches in any case`() {
        assertThat(bearerTokenRequests.matches(requestWithAuthorization("bearer eyJhbGciOiJSUzI1NiJ9"))).isTrue()
    }

    @Test
    fun `a request with a malformed bearer token matches`() {
        assertThat(bearerTokenRequests.matches(requestWithAuthorization("Bearer not a token"))).isTrue()
    }

    @Test
    fun `a request with another scheme does not match`() {
        assertThat(bearerTokenRequests.matches(requestWithAuthorization("Basic YWRhOnNlY3JldA=="))).isFalse()
    }

    @Test
    fun `a request without an Authorization header does not match`() {
        assertThat(bearerTokenRequests.matches(MockHttpServletRequest())).isFalse()
    }

    private fun requestWithAuthorization(value: String): MockHttpServletRequest =
        MockHttpServletRequest().apply { addHeader(HttpHeaders.AUTHORIZATION, value) }
}
