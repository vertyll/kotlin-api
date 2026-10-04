package com.vertyll.kotlinapi.auth

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.time.Clock

@Configuration
@EnableConfigurationProperties(KeycloakProperties::class, AuthProperties::class, RedisKeyProperties::class)
class AuthConfig {
    @Bean
    fun keycloakRestClient(): RestClient = RestClient.create()

    @Bean
    fun sessionTokenRelayFilter(
        browserSessions: BrowserSessions,
        sessions: SessionService,
        clock: Clock,
    ): FilterRegistrationBean<SessionTokenRelayFilter> =
        FilterRegistrationBean(SessionTokenRelayFilter(browserSessions, sessions, clock)).apply {
            order = SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1
        }
}
