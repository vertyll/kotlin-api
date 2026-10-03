package com.vertyll.kotlinapi

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer

@TestConfiguration(proxyBeanMethods = false)
class RedisTestContainer {
    @Bean
    @ServiceConnection(name = "redis")
    fun redisContainer(): GenericContainer<*> = GenericContainer("redis:8-alpine").withExposedPorts(REDIS_PORT)

    private companion object {
        private const val REDIS_PORT = 6379
    }
}
