package com.vertyll.kotlinapi.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.ClientAuthorizationException
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

@Component
class SessionAccessTokens(
    private val authorizedClients: OAuth2AuthorizedClientManager,
    private val accessTokens: JwtDecoder,
) {
    private val log = LoggerFactory.getLogger(SessionAccessTokens::class.java)

    fun current(
        session: OAuth2AuthenticationToken,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): Jwt? =
        try {
            authorizedClients
                .authorize(
                    OAuth2AuthorizeRequest
                        .withClientRegistrationId(session.authorizedClientRegistrationId)
                        .principal(session)
                        .attribute(HttpServletRequest::class.java.name, request)
                        .attribute(HttpServletResponse::class.java.name, response)
                        .build(),
                )?.let { accessTokens.decode(it.accessToken.tokenValue) }
        } catch (e: ClientAuthorizationException) {
            if (e.error.errorCode == OAuth2ErrorCodes.INVALID_GRANT) {
                request.getSession(false)?.invalidate()
                log.info("Session of {} ended: Keycloak refused the refresh", session.name)
            } else {
                log.warn("Could not refresh the session of {}: {}", session.name, e.error.errorCode)
            }
            null
        } catch (e: JwtException) {
            log.warn("Access token of {} rejected: {}", session.name, e.message)
            null
        }
}
