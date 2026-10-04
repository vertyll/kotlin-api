package com.vertyll.kotlinapi.auth

import com.vertyll.kotlinapi.common.exception.ApiException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.ExpectedCount.times
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Clock
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class KeycloakTokenClientTest {
    private val builder = RestClient.builder()
    private val keycloak = MockRestServiceServer.bindTo(builder).build()
    private val client =
        KeycloakTokenClient(
            KeycloakProperties("http://keycloak.test/realms/kotlin-api", "kotlin-api", "secret"),
            AuthProperties("http://app.test/auth/callback", "http://app.test/"),
            { token ->
                Jwt
                    .withTokenValue(token)
                    .header("alg", "RS256")
                    .subject("subject")
                    .claim("email", "ada@kotlin-api.local")
                    .claim("realm_access", mapOf("roles" to listOf("USER", "offline_access")))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build()
            },
            Clock.systemUTC(),
            builder.build(),
            SharedRefreshes.inProcessOnly(),
        )

    @Test
    fun `concurrent refreshes of one token reach Keycloak once`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        keycloak.expect(once(), requestTo(TOKEN_URL)).andRespond { request ->
            entered.countDown()
            assertTrue(release.await(5, TimeUnit.SECONDS))
            withSuccess(TOKENS, MediaType.APPLICATION_JSON).createResponse(request)
        }

        val first = CompletableFuture.supplyAsync { client.refresh("refresh-1") }
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        val second = CompletableFuture.supplyAsync { client.refresh("refresh-1") }
        release.countDown()

        assertEquals("refresh-2", first.get(5, TimeUnit.SECONDS).refreshToken)
        assertEquals("refresh-2", second.get(5, TimeUnit.SECONDS).refreshToken)
        assertEquals(setOf("USER"), first.get().identity.roles)
        keycloak.verify()
    }

    @Test
    fun `a session read before the refresh gets the tokens already issued`() {
        keycloak.expect(once(), requestTo(TOKEN_URL)).andRespond(withSuccess(TOKENS, MediaType.APPLICATION_JSON))

        client.refresh("refresh-1")

        assertEquals("access-2", client.refresh("refresh-1").accessToken)
        keycloak.verify()
    }

    @Test
    fun `a refused refresh ends the session and is not remembered`() {
        keycloak.expect(times(2), requestTo(TOKEN_URL)).andRespond(withBadRequest())

        val failure = assertThrows(ApiException::class.java) { client.refresh("refresh-1") }
        assertEquals(AuthErrors.SESSION_EXPIRED, failure.messageKey)
        assertThrows(ApiException::class.java) { client.refresh("refresh-1") }
        keycloak.verify()
    }

    private companion object {
        private const val TOKEN_URL = "http://keycloak.test/realms/kotlin-api/protocol/openid-connect/token"
        private const val TOKENS = """{"access_token":"access-2","refresh_token":"refresh-2"}"""
    }
}
