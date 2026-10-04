package com.vertyll.kotlinapi.auth

import com.vertyll.kotlinapi.PostgresTestContainer
import com.vertyll.kotlinapi.RedisTestContainer
import com.vertyll.kotlinapi.user.repository.UserRepository
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoderFactory
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.assertj.MvcTestResult
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
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
    private lateinit var jwtDecoder: JwtDecoder

    @MockitoBean
    private lateinit var idTokenDecoders: JwtDecoderFactory<ClientRegistration>

    @MockitoBean
    private lateinit var codeTokens: OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>

    @MockitoBean
    private lateinit var refreshTokens: OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest>

    @MockitoBean
    private lateinit var keycloakSessions: KeycloakSessions

    @BeforeEach
    fun keycloakIssuesTokens() {
        whenever(jwtDecoder.decode(ACCESS_TOKEN)).thenReturn(accessToken(ACCESS_TOKEN))
        whenever(jwtDecoder.decode(REFRESHED_ACCESS_TOKEN)).thenReturn(accessToken(REFRESHED_ACCESS_TOKEN))
    }

    @Test
    fun `authorize redirects to Keycloak with PKCE and the language`() {
        val result =
            mvc
                .get()
                .uri("/auth/authorize")
                .param("register", "true")
                .param("kc_action", "UPDATE_PASSWORD")
                .header("Accept-Language", "pl")
                .exchange()

        assertThat(result).hasStatus(HttpStatus.FOUND)
        assertThat(result.response.redirectedUrl).startsWith("http://localhost:9000/realms/kotlin-api/protocol/openid-connect/auth")
        val query = queryOf(result)
        assertThat(query["code_challenge_method"]).containsExactly("S256")
        assertThat(query.getFirst("code_challenge")).hasSize(CHALLENGE_LENGTH)
        assertThat(query["ui_locales"]).containsExactly("pl")
        assertThat(query["prompt"]).containsExactly("create")
        assertThat(query["kc_action"]).containsExactly("UPDATE_PASSWORD")
        assertThat(query["redirect_uri"]).containsExactly("http://localhost:8080/api/v1/auth/callback")
    }

    @Test
    fun `a callback with a state this browser was not given is refused`() {
        val browser = Browser()
        browser.send(mvc.get().uri("/auth/authorize"))

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
        val browser = signedIn(LIFETIME_SECONDS)

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
    fun `a sign-in with an unusable token revokes the new session`() {
        whenever(jwtDecoder.decode(ACCESS_TOKEN)).thenReturn(accessToken(ACCESS_TOKEN, email = null))
        val browser = Browser()

        val callback = callback(browser, keycloakAnswers(browser, LIFETIME_SECONDS))

        assertThat(callback.response.redirectedUrl).endsWith("error=sign_in_failed")
        verify(keycloakSessions).revoke(REFRESH_TOKEN)
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatus(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `a client with its own token calls the API without a session`() {
        signedIn(LIFETIME_SECONDS)

        assertThat(mvc.get().uri("/users/me").header("Authorization", "Bearer $ACCESS_TOKEN")).hasStatusOk()
    }

    @Test
    fun `an access token about to expire is refreshed`() {
        whenever(refreshTokens.getTokenResponse(any())).thenReturn(tokenResponse(REFRESHED_ACCESS_TOKEN, LIFETIME_SECONDS))
        val browser = signedIn(EXPIRING_SECONDS)

        assertThat(browser.send(mvc.get().uri("/users/me"))).hasStatusOk()
        verify(refreshTokens).getTokenResponse(any())
    }

    @Test
    fun `an expired session that cannot be refreshed ends`() {
        whenever(refreshTokens.getTokenResponse(any()))
            .thenThrow(OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT)))
        val browser = signedIn(EXPIRING_SECONDS)

        assertThat(browser.send(mvc.get().uri("/users/me"))).hasStatus(HttpStatus.UNAUTHORIZED)
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatus(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `a cross-site write is not given the session token`() {
        val browser = signedIn(LIFETIME_SECONDS)

        assertThat(browser.send(mvc.post().uri("/users/me").header("Sec-Fetch-Site", "cross-site")))
            .hasStatus(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `logout revokes the refresh token and ends the session`() {
        val browser = signedIn(LIFETIME_SECONDS)

        assertThat(browser.send(mvc.post().uri("/auth/logout"))).hasStatus(HttpStatus.NO_CONTENT)
        verify(keycloakSessions).revoke(REFRESH_TOKEN)
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatus(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `logout sent from another site leaves the session`() {
        val browser = signedIn(LIFETIME_SECONDS)

        assertThat(browser.send(mvc.post().uri("/auth/logout").header("Sec-Fetch-Site", "same-site")))
            .hasStatus(HttpStatus.FORBIDDEN)
        verify(keycloakSessions, never()).revoke(REFRESH_TOKEN)
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatus(HttpStatus.OK)
    }

    private fun signedIn(accessTokenLifetimeSeconds: Long): Browser {
        val browser = Browser()
        val state = keycloakAnswers(browser, accessTokenLifetimeSeconds)
        val preLogin = browser.sessionValues()

        val callback = callback(browser, state)

        assertThat(callback).hasStatus(HttpStatus.FOUND)
        assertThat(callback.response.redirectedUrl).doesNotContain("error")
        assertThat(browser.sessionValues()).doesNotContainAnyElementsOf(preLogin)
        return browser
    }

    private fun keycloakAnswers(
        browser: Browser,
        accessTokenLifetimeSeconds: Long,
    ): String {
        val authorization = queryOf(browser.send(mvc.get().uri("/auth/authorize")))
        val idToken = idToken(requireNotNull(authorization.getFirst("nonce")))
        whenever(idTokenDecoders.createDecoder(any())).thenReturn(JwtDecoder { idToken })
        whenever(codeTokens.getTokenResponse(any())).thenReturn(tokenResponse(ACCESS_TOKEN, accessTokenLifetimeSeconds))
        return requireNotNull(authorization.getFirst("state"))
    }

    private fun callback(
        browser: Browser,
        state: String,
    ): MvcTestResult =
        browser.send(
            mvc
                .get()
                .uri("/auth/callback")
                .param(CODE, CODE)
                .param("state", state),
        )

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

    private fun tokenResponse(
        accessToken: String,
        lifetimeSeconds: Long,
    ): OAuth2AccessTokenResponse =
        OAuth2AccessTokenResponse
            .withToken(accessToken)
            .tokenType(OAuth2AccessToken.TokenType.BEARER)
            .expiresIn(lifetimeSeconds)
            .refreshToken(REFRESH_TOKEN)
            .scopes(setOf("openid", "profile", "email"))
            .additionalParameters(mapOf(OidcParameterNames.ID_TOKEN to ID_TOKEN))
            .build()

    private fun idToken(nonce: String): Jwt =
        Jwt
            .withTokenValue(ID_TOKEN)
            .header("alg", "RS256")
            .issuer("http://localhost:9000/realms/kotlin-api")
            .subject(SUBJECT)
            .audience(listOf("kotlin-api"))
            .claim("email", EMAIL)
            .claim("nonce", nonce)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(LIFETIME_SECONDS))
            .build()

    private fun accessToken(
        value: String,
        email: String? = EMAIL,
    ): Jwt =
        Jwt
            .withTokenValue(value)
            .header("alg", "RS256")
            .subject(SUBJECT)
            .apply { email?.let { claim("email", it) } }
            .claim("given_name", "Ada")
            .claim("family_name", "Lovelace")
            .claim("realm_access", mapOf("roles" to listOf("ADMIN")))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(LIFETIME_SECONDS))
            .build()

    private fun queryOf(result: MvcTestResult): MultiValueMap<String, String> =
        UriComponentsBuilder
            .fromUri(URI.create(requireNotNull(result.response.redirectedUrl)))
            .build()
            .queryParams
            .mapValuesTo(LinkedMultiValueMap()) { (_, values) -> values.map { URLDecoder.decode(it, StandardCharsets.UTF_8) } }

    private companion object {
        private const val ACCESS_TOKEN = "access-token"
        private const val REFRESHED_ACCESS_TOKEN = "refreshed-access-token"
        private const val REFRESH_TOKEN = "refresh-token"
        private const val ID_TOKEN = "id-token"
        private const val CODE = "code"
        private const val SUBJECT = "7d1c9a52-3a43-4c34-9a8f-2f5d7c6f1b10"
        private const val EMAIL = "ada@kotlin-api.local"
        private const val CHALLENGE_LENGTH = 43
        private const val LIFETIME_SECONDS = 300L
        private const val EXPIRING_SECONDS = 5L
    }
}
