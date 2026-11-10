package com.vertyll.kotlinapi.auth

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.data.redis.serializer.SerializationException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.web.savedrequest.SimpleSavedRequest
import java.net.URI
import java.time.Instant

class SessionStoreConfigTest {
    private val serializer = SessionStoreConfig.sessionSerializer()

    @Test
    fun theAuthorizedClientWithItsTokensSurvivesARoundTripThroughTheStore() {
        val clients = hashMapOf(REGISTRATION_ID to authorizedClient())

        assertThat(serializer.serialize(clients).first()).isEqualTo('{'.code.toByte())
        val client = roundTrip<Map<*, *>>(clients)[REGISTRATION_ID] as? OAuth2AuthorizedClient ?: error("no client")
        assertThat(client.principalName).isEqualTo("ada")
        assertThat(client.clientRegistration.registrationId).isEqualTo(REGISTRATION_ID)
        assertThat(client.accessToken.tokenValue).isEqualTo("access-token")
        assertThat(client.accessToken.expiresAt).isEqualTo(EXPIRES)
        assertThat(client.accessToken.scopes).containsExactlyInAnyOrder("openid", "email")
        assertThat(client.refreshToken?.tokenValue).isEqualTo("refresh-token")
    }

    @Test
    fun theSecurityContextOfASignInSurvivesARoundTripThroughTheStore() {
        val idToken = OidcIdToken("id-token", ISSUED, EXPIRES, mapOf("sub" to "ada", "iss" to URI.create("https://kc").toURL()))
        val user = DefaultOidcUser(listOf(SimpleGrantedAuthority("OIDC_USER")), idToken)
        val context = SecurityContextImpl(OAuth2AuthenticationToken(user, user.authorities, REGISTRATION_ID))

        val read = roundTrip<SecurityContextImpl>(context)

        val authentication = read.authentication as? OAuth2AuthenticationToken ?: error("no sign-in")
        assertThat(authentication.name).isEqualTo("ada")
        assertThat(authentication.authorizedClientRegistrationId).isEqualTo(REGISTRATION_ID)
        assertThat(authentication.isAuthenticated).isTrue()
    }

    @Test
    fun aSignInInProgressSurvivesARoundTripThroughTheStore() {
        val request =
            OAuth2AuthorizationRequest
                .authorizationCode()
                .authorizationUri("https://kc/auth")
                .clientId("kotlin-api")
                .redirectUri("http://localhost:8080/api/v1/auth/callback")
                .scopes(setOf("openid"))
                .state("state")
                .build()

        val read = roundTrip<OAuth2AuthorizationRequest>(request)

        assertThat(read.state).isEqualTo("state")
        assertThat(read.redirectUri).isEqualTo("http://localhost:8080/api/v1/auth/callback")
    }

    @Test
    fun thePageASignInReturnsToSurvivesARoundTripThroughTheStore() {
        val read = roundTrip<SimpleSavedRequest>(SimpleSavedRequest("http://localhost:8080/post/7"))

        assertThat(read.redirectUrl).isEqualTo("http://localhost:8080/post/7")
        assertThat(read.method).isEqualTo("GET")
    }

    @Test
    fun aStoredValueNamingAClassOutsideTheAllowedOnesIsRefused() {
        val stored = """{"@class":"java.lang.ProcessBuilder","command":["true"]}""".toByteArray()

        assertThatThrownBy { serializer.deserialize(stored) }.isInstanceOf(SerializationException::class.java)
    }

    private inline fun <reified T> roundTrip(value: Any): T =
        serializer.deserialize(serializer.serialize(value)) as? T ?: error("read back as something else")

    private fun authorizedClient(): OAuth2AuthorizedClient {
        val registration =
            ClientRegistration
                .withRegistrationId(REGISTRATION_ID)
                .clientId("kotlin-api")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/api/v1/auth/callback")
                .scope("openid", "email")
                .authorizationUri("https://kc/auth")
                .tokenUri("https://kc/token")
                .jwkSetUri("https://kc/certs")
                .build()
        val accessToken =
            OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token", ISSUED, EXPIRES, setOf("openid", "email"))
        return OAuth2AuthorizedClient(registration, "ada", accessToken, OAuth2RefreshToken("refresh-token", ISSUED))
    }

    private companion object {
        const val REGISTRATION_ID = "keycloak"
        val ISSUED: Instant = Instant.parse("2026-11-10T08:00:00Z")
        val EXPIRES: Instant = ISSUED.plusSeconds(300)
    }
}
