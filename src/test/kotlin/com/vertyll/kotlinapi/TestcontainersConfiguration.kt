package com.vertyll.kotlinapi

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Duration

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer =
        PostgreSQLContainer("postgres:18-alpine").withStartupTimeout(Duration.ofMinutes(2)).withStartupAttempts(3)

    @Bean
    @ServiceConnection(name = "redis")
    fun redisContainer(): GenericContainer<*> =
        GenericContainer("redis:8-alpine").withStartupTimeout(Duration.ofMinutes(2)).withStartupAttempts(3).withExposedPorts(REDIS_PORT)

    private companion object {
        private const val REDIS_PORT = 6379
    }
}
