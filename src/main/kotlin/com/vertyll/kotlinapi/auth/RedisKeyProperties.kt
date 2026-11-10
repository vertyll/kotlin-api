package com.vertyll.kotlinapi.auth

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "application.redis")
data class RedisKeyProperties(
    @field:NotBlank val keyPrefix: String,
)
