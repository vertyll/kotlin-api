package com.vertyll.kotlinapi.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.time.Clock

@Configuration
@EnableConfigurationProperties(FrontendProperties::class, MailProperties::class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
class BeansConfig {
    @Bean
    fun auditorAware(): AuditorAware<String> = ApplicationAuditAware()

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
