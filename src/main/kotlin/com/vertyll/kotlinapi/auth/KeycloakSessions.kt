package com.vertyll.kotlinapi.auth

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class KeycloakSessions(
    private val keycloak: KeycloakProperties,
    private val restClient: RestClient,
) {
    private val log = LoggerFactory.getLogger(KeycloakSessions::class.java)

    fun revoke(refreshToken: String) {
        val form =
            LinkedMultiValueMap<String, String>().apply {
                add("client_id", keycloak.clientId)
                add("client_secret", keycloak.clientSecret)
                add("refresh_token", refreshToken)
            }
        try {
            restClient
                .post()
                .uri(keycloak.endpoint("logout"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientException) {
            log.warn("Keycloak did not end the session: {}", e.message)
        }
    }
}
