package com.vertyll.kotlinapi.config

import com.vertyll.kotlinapi.auth.HostedSignInRequests
import com.vertyll.kotlinapi.auth.KeycloakIdentity
import com.vertyll.kotlinapi.auth.SessionAccessTokenFilter
import com.vertyll.kotlinapi.auth.SessionAccessTokens
import com.vertyll.kotlinapi.auth.SignInCompletion
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.core.oidc.StandardClaimNames
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val corsConfigurationSource: CorsConfigurationSource,
    private val problemSecurityHandlers: ProblemSecurityHandlers,
) {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        clientRegistrations: ClientRegistrationRepository,
        authorizedClients: OAuth2AuthorizedClientRepository,
        authorizationCodeTokens: OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>,
        signInCompletion: SignInCompletion,
        sessionAccessTokens: SessionAccessTokens,
    ): SecurityFilterChain {
        val tokenAuthentication = keycloakTokens()
        http
            .cors { it.configurationSource(corsConfigurationSource) }
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/auth/**",
                        "/legal/**",
                        "/actuator/health/**",
                        "/translations/**",
                        "/roles/types",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                    ).permitAll()
                    .anyRequest()
                    .authenticated()
            }.oauth2Login { login ->
                login
                    .loginPage(HostedSignInRequests.AUTHORIZE_PATH)
                    .authorizationEndpoint { it.authorizationRequestResolver(HostedSignInRequests(clientRegistrations)) }
                    .redirectionEndpoint { it.baseUri(CALLBACK_PATH) }
                    .tokenEndpoint { it.accessTokenResponseClient(authorizationCodeTokens) }
                    .authorizedClientRepository(authorizedClients)
                    .successHandler(signInCompletion)
                    .failureHandler(signInCompletion)
            }.oauth2ResourceServer { oauth2 ->
                oauth2
                    .jwt { it.jwtAuthenticationConverter(tokenAuthentication) }
                    .authenticationEntryPoint(problemSecurityHandlers)
                    .accessDeniedHandler(problemSecurityHandlers)
            }.exceptionHandling {
                it
                    .authenticationEntryPoint(problemSecurityHandlers)
                    .accessDeniedHandler(problemSecurityHandlers)
            }.requestCache(RequestCacheConfigurer<HttpSecurity>::disable)
            .addFilterBefore(
                SessionAccessTokenFilter(sessionAccessTokens, tokenAuthentication),
                AnonymousAuthenticationFilter::class.java,
            )

        return http.build()
    }

    private fun keycloakTokens(): JwtAuthenticationConverter =
        JwtAuthenticationConverter().apply {
            setPrincipalClaimName(StandardClaimNames.EMAIL)
            setJwtGrantedAuthoritiesConverter { jwt ->
                KeycloakIdentity.realmRoles(jwt).map { SimpleGrantedAuthority("ROLE_$it") }
            }
        }

    private companion object {
        private const val CALLBACK_PATH = "/auth/callback"
    }
}
