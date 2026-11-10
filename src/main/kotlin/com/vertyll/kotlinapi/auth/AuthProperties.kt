package com.vertyll.kotlinapi.auth

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "application.auth")
data class AuthProperties(
    @field:NotBlank val callbackUrl: String,
    @field:NotBlank val postLoginUrl: String,
)
