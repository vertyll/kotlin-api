package com.vertyll.kotlinapi.auth

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "application.keycloak")
data class KeycloakProperties(
    @field:NotBlank val realmUrl: String,
    @field:NotBlank val clientId: String,
    @field:NotBlank val clientSecret: String,
) {
    fun endpoint(name: String): String = "$realmUrl/protocol/openid-connect/$name"

    override fun toString(): String = "KeycloakProperties(realmUrl=$realmUrl, clientId=$clientId, clientSecret=***)"
}
