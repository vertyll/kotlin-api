package com.vertyll.kotlinapi.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "application.frontend")
data class FrontendProperties(
    @field:NotBlank val url: String,
)
