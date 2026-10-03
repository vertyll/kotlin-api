package com.vertyll.kotlinapi.auth

import com.vertyll.kotlinapi.common.exception.ApiException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder
import java.util.Locale

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Sign-in through Keycloak's hosted pages")
class AuthController(
    private val sessions: SessionService,
    private val browserSessions: BrowserSessions,
    private val keycloak: KeycloakProperties,
    private val auth: AuthProperties,
) {
    private val log = LoggerFactory.getLogger(AuthController::class.java)

    @GetMapping("/authorize")
    @Operation(summary = "Redirect to Keycloak to sign in or register")
    fun authorize(
        request: HttpServletRequest,
        locale: Locale,
        @RequestParam(name = "kc_action", required = false) kcAction: String?,
        @RequestParam(defaultValue = "false") register: Boolean,
    ): ResponseEntity<Void> {
        val transaction = browserSessions.begin(request)
        val uri =
            UriComponentsBuilder
                .fromUriString(keycloak.endpoint("auth"))
                .queryParam("client_id", keycloak.clientId)
                .queryParam("redirect_uri", auth.callbackUrl)
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPE)
                .queryParam("state", transaction.state)
                .queryParam("code_challenge", Pkce.challengeOf(transaction.codeVerifier))
                .queryParam("code_challenge_method", Pkce.CHALLENGE_METHOD)
        if (locale.language in UI_LOCALES) {
            uri.queryParam("ui_locales", locale.language)
        }
        if (kcAction != null && kcAction in ALLOWED_ACTIONS) {
            uri.queryParam("kc_action", kcAction)
        }
        if (register) {
            uri.queryParam("prompt", "create")
        }
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(
                uri
                    .encode()
                    .build()
                    .toUri(),
            ).build()
    }

    @GetMapping("/callback")
    @Operation(summary = "Finish signing in after Keycloak redirects back")
    fun callback(
        request: HttpServletRequest,
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) state: String?,
        @RequestParam(name = ERROR_PARAM, required = false) error: String?,
    ): ResponseEntity<Void> {
        val transaction = browserSessions.takeTransaction(request)
        return when {
            error != null -> {
                log.debug("Keycloak returned an authorization error: {}", error)
                redirectToApp(SIGN_IN_FAILED)
            }

            code == null || state == null || transaction == null || !Pkce.sameState(transaction.state, state) -> {
                log.warn("Rejecting a sign-in callback whose state was not issued to this browser")
                redirectToApp(STATE_MISMATCH)
            }

            else -> {
                completeSignIn(request, code, transaction)
            }
        }
    }

    @GetMapping("/session")
    @Operation(summary = "The signed-in user, or 204 without a session")
    fun session(request: HttpServletRequest): ResponseEntity<SessionResponseDto> =
        browserSessions.current(request)?.let { ResponseEntity.ok(SessionResponseDto.from(it)) }
            ?: ResponseEntity.noContent().build()

    @PostMapping("/logout")
    @Operation(summary = "End the session here and at Keycloak")
    fun logout(request: HttpServletRequest): ResponseEntity<Void> {
        if (!FetchMetadata.sentFromThisOrigin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        browserSessions.current(request)?.let(sessions::signOut)
        browserSessions.end(request)
        return ResponseEntity.noContent().build()
    }

    private fun completeSignIn(
        request: HttpServletRequest,
        code: String,
        transaction: SignInTransaction,
    ): ResponseEntity<Void> =
        try {
            browserSessions.establish(request, sessions.signIn(code, transaction.codeVerifier))
            redirectToApp(null)
        } catch (e: ApiException) {
            log.warn("Sign-in could not be completed: {}", e.messageKey)
            redirectToApp(SIGN_IN_FAILED)
        }

    private fun redirectToApp(errorCode: String?): ResponseEntity<Void> {
        val uri = UriComponentsBuilder.fromUriString(auth.postLoginUrl)
        errorCode?.let { uri.queryParam(ERROR_PARAM, it) }
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(uri.build().toUri())
            .build()
    }

    private companion object {
        private const val SCOPE = "openid profile email"
        private const val ERROR_PARAM = "error"
        private const val SIGN_IN_FAILED = "sign_in_failed"
        private const val STATE_MISMATCH = "state_mismatch"
        private val ALLOWED_ACTIONS = setOf("CONFIGURE_TOTP", "UPDATE_PASSWORD", "delete_credential")
        private val UI_LOCALES = setOf("pl", "en")
    }
}
