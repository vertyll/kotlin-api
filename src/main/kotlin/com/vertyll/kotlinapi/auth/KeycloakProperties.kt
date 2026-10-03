package com.vertyll.kotlinapi.auth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "application.keycloak")
data class KeycloakProperties(
    val serverUrl: String,
    val realm: String,
    val clientId: String,
    val clientSecret: String,
) {
    fun endpoint(name: String): String = "$serverUrl/realms/$realm/protocol/openid-connect/$name"

    override fun toString(): String = "KeycloakProperties(serverUrl=$serverUrl, realm=$realm, clientId=$clientId, clientSecret=***)"
}
