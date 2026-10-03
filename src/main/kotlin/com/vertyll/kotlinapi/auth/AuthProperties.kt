package com.vertyll.kotlinapi.auth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "application.auth")
data class AuthProperties(
    val callbackUrl: String,
    val postLoginUrl: String,
)
