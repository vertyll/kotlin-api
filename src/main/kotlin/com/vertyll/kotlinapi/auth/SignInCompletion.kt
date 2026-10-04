package com.vertyll.kotlinapi.auth

import com.vertyll.kotlinapi.common.exception.ApiException
import com.vertyll.kotlinapi.user.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class SignInCompletion(
    private val authorizedClients: OAuth2AuthorizedClientRepository,
    private val accessTokens: JwtDecoder,
    private val users: UserService,
    private val keycloakSessions: KeycloakSessions,
    private val auth: AuthProperties,
) : AuthenticationSuccessHandler,
    AuthenticationFailureHandler {
    private val log = LoggerFactory.getLogger(SignInCompletion::class.java)

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val signIn = authentication as? OAuth2AuthenticationToken
        val client =
            signIn?.let {
                authorizedClients.loadAuthorizedClient<OAuth2AuthorizedClient>(it.authorizedClientRegistrationId, it, request)
            }
        val refreshToken = client?.refreshToken?.tokenValue
        if (client == null || refreshToken == null) {
            fail(request, response, authentication, "Keycloak issued no refresh token")
            return
        }
        val failure =
            try {
                val identity = KeycloakIdentity.from(accessTokens.decode(client.accessToken.tokenValue))
                users.sync(identity)
                log.info("User {} signed in", identity.keycloakId)
                null
            } catch (e: JwtException) {
                e
            } catch (e: IllegalArgumentException) {
                e
            } catch (e: ApiException) {
                e
            } catch (e: DataAccessException) {
                e
            }
        if (failure != null) {
            keycloakSessions.revoke(refreshToken)
            fail(request, response, authentication, failure.message)
            return
        }
        response.sendRedirect(auth.postLoginUrl)
    }

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        val errorCode = (exception as? OAuth2AuthenticationException)?.error?.errorCode
        log.warn("Sign-in could not be completed: {}", errorCode ?: exception.message)
        redirectWithError(response, if (errorCode in STATE_ERRORS) STATE_MISMATCH else SIGN_IN_FAILED)
    }

    private fun fail(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
        reason: String?,
    ) {
        log.warn("Sign-in could not be completed: {}", reason)
        SecurityContextLogoutHandler().logout(request, response, authentication)
        redirectWithError(response, SIGN_IN_FAILED)
    }

    private fun redirectWithError(
        response: HttpServletResponse,
        errorCode: String,
    ) {
        response.sendRedirect(
            UriComponentsBuilder
                .fromUriString(auth.postLoginUrl)
                .queryParam("error", errorCode)
                .build()
                .toUriString(),
        )
    }

    private companion object {
        private const val SIGN_IN_FAILED = "sign_in_failed"
        private const val STATE_MISMATCH = "state_mismatch"
        private val STATE_ERRORS = setOf("authorization_request_not_found", "invalid_state_parameter")
    }
}
