package com.vertyll.kotlinapi.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "application.frontend")
data class FrontendProperties(
    val url: String,
)
