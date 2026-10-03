package com.vertyll.kotlinapi.auth

import com.vertyll.kotlinapi.PostgresTestContainer
import com.vertyll.kotlinapi.RedisTestContainer
import com.vertyll.kotlinapi.user.repository.UserRepository
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.assertj.MvcTestResult
import org.springframework.util.MultiValueMap
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainer::class, RedisTestContainer::class)
class HostedSignInTest {
    @Autowired
    private lateinit var mvc: MockMvcTester

    @Autowired
    private lateinit var users: UserRepository

    @MockitoBean
    private lateinit var tokenClient: KeycloakTokenClient

    @MockitoBean
    private lateinit var jwtDecoder: JwtDecoder

    @Test
    fun `authorize redirects to Keycloak with PKCE and the language`() {
        val result =
            mvc
                .get()
                .uri("/auth/authorize")
                .param("register", "true")
                .header("Accept-Language", "pl")
                .exchange()

        assertThat(result).hasStatus(HttpStatus.FOUND)
        assertThat(result.response.redirectedUrl).startsWith("http://localhost:9000/realms/kotlin-api/protocol/openid-connect/auth")
        val query = queryOf(result)
        assertThat(query["code_challenge_method"]).containsExactly("S256")
        assertThat(query.getFirst("code_challenge")).hasSize(CHALLENGE_LENGTH)
        assertThat(query["ui_locales"]).containsExactly("pl")
        assertThat(query["prompt"]).containsExactly("create")
        assertThat(query["redirect_uri"]).containsExactly("http://localhost:8080/api/v1/auth/callback")
    }

    @Test
    fun `a callback with a state this browser was not given is refused`() {
        val browser = Browser()
        stateIssuedTo(browser)

        val result =
            browser.send(
                mvc
                    .get()
                    .uri("/auth/callback")
                    .param(CODE, CODE)
                    .param("state", "forged"),
            )

        assertThat(result).hasStatus(HttpStatus.FOUND)
        assertThat(result.response.redirectedUrl).endsWith("error=state_mismatch")
    }

    @Test
    fun `signing in provisions the account and relays the token from the session`() {
        whenever(jwtDecoder.decode(ACCESS_TOKEN)).thenReturn(jwt())

        val browser = signedIn(session(Instant.now().plusSeconds(LIFETIME_SECONDS)))

        assertThat(users.findByKeycloakId(SUBJECT)).isPresent
        assertThat(browser.send(mvc.get().uri("/auth/session")))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$.email")
            .isEqualTo(EMAIL)
        assertThat(browser.send(mvc.get().uri("/users/me"))).hasStatusOk()
        assertThat(mvc.get().uri("/users/me")).hasStatus(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `an expired session that cannot be refreshed ends`() {
        whenever(tokenClient.refresh(any())).thenThrow(AuthErrors.rejected(AuthErrors.SESSION_EXPIRED))
        val browser = signedIn(session(Instant.now().minusSeconds(EXPIRED_SECONDS)))

        assertThat(browser.send(mvc.get().uri("/users/me"))).hasStatus(HttpStatus.UNAUTHORIZED)
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatus(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `a cross-site write is not given the session token`() {
        whenever(jwtDecoder.decode(ACCESS_TOKEN)).thenReturn(jwt())
        val browser = signedIn(session(Instant.now().plusSeconds(LIFETIME_SECONDS)))

        assertThat(browser.send(mvc.post().uri("/users/me").header("Sec-Fetch-Site", "cross-site")))
            .hasStatus(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `logout revokes the refresh token and ends the session`() {
        val browser = signedIn(session(Instant.now().plusSeconds(LIFETIME_SECONDS)))

        assertThat(browser.send(mvc.post().uri("/auth/logout"))).hasStatus(HttpStatus.NO_CONTENT)
        verify(tokenClient).revoke(REFRESH_TOKEN)
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatus(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `logout sent from another site leaves the session`() {
        val browser = signedIn(session(Instant.now().plusSeconds(LIFETIME_SECONDS)))

        assertThat(browser.send(mvc.post().uri("/auth/logout").header("Sec-Fetch-Site", "same-site")))
            .hasStatus(HttpStatus.FORBIDDEN)
        verify(tokenClient, never()).revoke(REFRESH_TOKEN)
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatus(HttpStatus.OK)
    }

    private fun stateIssuedTo(browser: Browser): String =
        requireNotNull(queryOf(browser.send(mvc.get().uri("/auth/authorize"))).getFirst("state"))

    private fun signedIn(authSession: AuthSession): Browser {
        whenever(tokenClient.exchange(eq(CODE), anyOrNull())).thenReturn(authSession)
        val browser = Browser()
        val state = stateIssuedTo(browser)
        val preLogin = browser.sessionValues()

        val callback =
            browser.send(
                mvc
                    .get()
                    .uri("/auth/callback")
                    .param(CODE, CODE)
                    .param("state", state),
            )

        assertThat(callback).hasStatus(HttpStatus.FOUND)
        assertThat(callback.response.redirectedUrl).doesNotContain("error")
        assertThat(browser.sessionValues()).doesNotContainAnyElementsOf(preLogin)
        return browser
    }

    private class Browser {
        private val cookies = linkedMapOf<String, Cookie>()

        fun send(request: MockMvcTester.MockMvcRequestBuilder): MvcTestResult {
            if (cookies.isNotEmpty()) {
                request.cookie(*cookies.values.toTypedArray())
            }
            val result = request.exchange()
            result.response.cookies.forEach {
                if (it.maxAge == 0) cookies.remove(it.name) else cookies[it.name] = it
            }
            return result
        }

        fun sessionValues(): List<String> = cookies.values.map { it.value }
    }

    private fun session(expiresAt: Instant): AuthSession =
        AuthSession(KeycloakIdentity(SUBJECT, EMAIL, "Ada", "Lovelace", setOf("ADMIN")), ACCESS_TOKEN, REFRESH_TOKEN, expiresAt)

    private fun jwt(): Jwt =
        Jwt
            .withTokenValue(ACCESS_TOKEN)
            .header("alg", "RS256")
            .subject(SUBJECT)
            .claim("email", EMAIL)
            .claim("realm_access", mapOf("roles" to listOf("ADMIN")))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(LIFETIME_SECONDS))
            .build()

    private fun queryOf(result: MvcTestResult): MultiValueMap<String, String> =
        UriComponentsBuilder.fromUri(URI.create(requireNotNull(result.response.redirectedUrl))).build().queryParams

    private companion object {
        private const val ACCESS_TOKEN = "access-token"
        private const val REFRESH_TOKEN = "refresh-token"
        private const val CODE = "code"
        private const val SUBJECT = "7d1c9a52-3a43-4c34-9a8f-2f5d7c6f1b10"
        private const val EMAIL = "ada@kotlin-api.local"
        private const val CHALLENGE_LENGTH = 43
        private const val LIFETIME_SECONDS = 300L
        private const val EXPIRED_SECONDS = 5L
    }
}
