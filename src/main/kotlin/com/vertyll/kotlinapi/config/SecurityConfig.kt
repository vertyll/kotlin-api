package com.vertyll.kotlinapi.config

import com.vertyll.kotlinapi.auth.KeycloakIdentity
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.oidc.StandardClaimNames
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val corsConfigurationSource: CorsConfigurationSource,
    private val problemSecurityHandlers: ProblemSecurityHandlers,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
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
            }.oauth2ResourceServer { oauth2 ->
                oauth2
                    .jwt { it.jwtAuthenticationConverter(keycloakTokens()) }
                    .authenticationEntryPoint(problemSecurityHandlers)
                    .accessDeniedHandler(problemSecurityHandlers)
            }.exceptionHandling {
                it
                    .authenticationEntryPoint(problemSecurityHandlers)
                    .accessDeniedHandler(problemSecurityHandlers)
            }.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        return http.build()
    }

    private fun keycloakTokens(): JwtAuthenticationConverter =
        JwtAuthenticationConverter().apply {
            setPrincipalClaimName(StandardClaimNames.EMAIL)
            setJwtGrantedAuthoritiesConverter { jwt ->
                KeycloakIdentity.realmRoles(jwt).map { SimpleGrantedAuthority("ROLE_$it") }
            }
        }
}
