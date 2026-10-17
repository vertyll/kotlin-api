package com.vertyll.kotlinapi.auth

import com.vertyll.kotlinapi.translation.model.Language
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
                Language.fromCode(request.locale.language)?.let { parameters["ui_locales"] = it.code }
                request.getParameter(KC_ACTION)?.takeIf(::isAccountAction)?.let { parameters[KC_ACTION] = it }
                if (request.getParameter("register").toBoolean()) {
                    parameters["prompt"] = "create"
                }
            }.build()
    }

    companion object {
        const val REGISTRATION_ID = "keycloak"
        const val AUTHORIZE_PATH = "/auth/authorize"
        private const val KC_ACTION = "kc_action"
        private val ACCOUNT_ACTIONS = setOf("CONFIGURE_TOTP", "UPDATE_PASSWORD")

        // Keycloak removes a credential only when named by its id, and asks the person to confirm first.
        private val DELETE_CREDENTIAL =
            Regex("delete_credential:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

        private fun isAccountAction(action: String): Boolean = action in ACCOUNT_ACTIONS || DELETE_CREDENTIAL.matches(action)
    }
}
