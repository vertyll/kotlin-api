package com.vertyll.kotlinapi.auth

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.DelegatingOAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.RestClientRefreshTokenTokenResponseClient
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.web.client.RestClient
import java.time.Clock

@Configuration
@EnableConfigurationProperties(KeycloakProperties::class, AuthProperties::class, RedisKeyProperties::class)
class AuthConfig {
    @Bean
    fun keycloakRestClient(): RestClient = RestClient.create()

    @Bean
    fun clientRegistrationRepository(
        keycloak: KeycloakProperties,
        auth: AuthProperties,
    ): ClientRegistrationRepository =
        InMemoryClientRegistrationRepository(
            ClientRegistration
                .withRegistrationId(HostedSignInRequests.REGISTRATION_ID)
                .clientName("Keycloak")
                .clientId(keycloak.clientId)
                .clientSecret(keycloak.clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(auth.callbackUrl)
                .scope(OidcScopes.OPENID, OidcScopes.PROFILE, OidcScopes.EMAIL)
                .authorizationUri(keycloak.endpoint("auth"))
                .tokenUri(keycloak.endpoint("token"))
                .jwkSetUri(keycloak.endpoint("certs"))
                .issuerUri(keycloak.realmUrl)
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .build(),
        )

    @Bean
    fun authorizedClientRepository(): OAuth2AuthorizedClientRepository = HttpSessionOAuth2AuthorizedClientRepository()

    @Bean
    fun authorizationCodeTokenResponseClient(): OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> =
        RestClientAuthorizationCodeTokenResponseClient()

    @Bean
    fun refreshTokenResponseClient(): OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> =
        RestClientRefreshTokenTokenResponseClient()

    @Bean
    fun authorizedClientManager(
        clientRegistrations: ClientRegistrationRepository,
        authorizedClients: OAuth2AuthorizedClientRepository,
        refreshTokenResponseClient: OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest>,
        sharedRefreshes: SharedRefreshes,
        clock: Clock,
    ): OAuth2AuthorizedClientManager =
        DefaultOAuth2AuthorizedClientManager(clientRegistrations, authorizedClients).apply {
            setAuthorizedClientProvider(
                DelegatingOAuth2AuthorizedClientProvider(
                    OAuth2AuthorizedClientProviderBuilder.builder().authorizationCode().build(),
                    SingleFlightRefreshTokenProvider(
                        OAuth2AuthorizedClientProviderBuilder
                            .builder()
                            .refreshToken { it.accessTokenResponseClient(refreshTokenResponseClient) }
                            .build(),
                        sharedRefreshes,
                        clock,
                    ),
                ),
            )
        }
}
