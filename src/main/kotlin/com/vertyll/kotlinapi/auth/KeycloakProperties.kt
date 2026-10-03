package com.vertyll.kotlinapi.auth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "application.keycloak")
data class KeycloakProperties(
    val realmUrl: String,
    val clientId: String,
    val clientSecret: String,
) {
    fun endpoint(name: String): String = "$realmUrl/protocol/openid-connect/$name"

    override fun toString(): String = "KeycloakProperties(realmUrl=$realmUrl, clientId=$clientId, clientSecret=***)"
}
