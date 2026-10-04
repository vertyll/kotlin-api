package com.vertyll.kotlinapi.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest

class HostedSignInRequests(
    registrations: ClientRegistrationRepository,
) : OAuth2AuthorizationRequestResolver {
    private val delegate =
        DefaultOAuth2AuthorizationRequestResolver(
            registrations,
            OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI,
        ).apply { setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce()) }

    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        if (request.requestURI == request.contextPath + AUTHORIZE_PATH) resolve(request, REGISTRATION_ID) else null

    override fun resolve(
        request: HttpServletRequest,
        clientRegistrationId: String,
    ): OAuth2AuthorizationRequest? {
        val authorization = delegate.resolve(request, clientRegistrationId) ?: return null
        return OAuth2AuthorizationRequest
            .from(authorization)
            .additionalParameters { parameters ->
                request.locale.language
                    .takeIf { it in UI_LOCALES }
                    ?.let { parameters["ui_locales"] = it }
                request.getParameter(KC_ACTION)?.takeIf { it in ALLOWED_ACTIONS }?.let { parameters[KC_ACTION] = it }
                if (request.getParameter("register").toBoolean()) {
                    parameters["prompt"] = "create"
                }
            }.build()
    }

    companion object {
        const val REGISTRATION_ID = "keycloak"
        const val AUTHORIZE_PATH = "/auth/authorize"
        private const val KC_ACTION = "kc_action"
        private val ALLOWED_ACTIONS = setOf("CONFIGURE_TOTP", "UPDATE_PASSWORD", "delete_credential")
        private val UI_LOCALES = setOf("pl", "en")
    }
}
