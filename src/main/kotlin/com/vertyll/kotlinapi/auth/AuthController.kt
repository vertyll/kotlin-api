package com.vertyll.kotlinapi.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Sign-in through Keycloak's hosted pages")
class AuthController(
    private val authorizedClients: OAuth2AuthorizedClientRepository,
    private val keycloakSessions: KeycloakSessions,
) {
    private val log = LoggerFactory.getLogger(AuthController::class.java)

    @GetMapping("/session")
    @Operation(summary = "The signed-in user, or 204 without a session")
    fun session(authentication: Authentication?): ResponseEntity<SessionResponseDto> =
        (authentication as? JwtAuthenticationToken)
            ?.let { ResponseEntity.ok(SessionResponseDto.from(KeycloakIdentity.from(it.token))) }
            ?: ResponseEntity.noContent().build()

    @PostMapping("/logout")
    @Operation(summary = "End the session here and at Keycloak")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?,
    ): ResponseEntity<Void> {
        if (!FetchMetadata.sentFromThisOrigin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        if (authentication != null) {
            authorizedClients
                .loadAuthorizedClient<OAuth2AuthorizedClient>(HostedSignInRequests.REGISTRATION_ID, authentication, request)
                ?.refreshToken
                ?.let { keycloakSessions.revoke(it.tokenValue) }
            log.info("User {} signed out", authentication.name)
        }
        SecurityContextLogoutHandler().logout(request, response, authentication)
        return ResponseEntity.noContent().build()
    }
}
