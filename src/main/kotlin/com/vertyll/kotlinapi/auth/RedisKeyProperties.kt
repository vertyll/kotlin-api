package com.vertyll.kotlinapi.auth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "application.redis")
data class RedisKeyProperties(
    val keyPrefix: String,
)
