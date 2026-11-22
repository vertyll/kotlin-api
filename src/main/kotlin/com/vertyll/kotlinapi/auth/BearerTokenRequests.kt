package com.vertyll.kotlinapi.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.util.matcher.RequestMatcher

/**
 * Requests authenticated by a bearer token, which need no CSRF token: no browser adds an `Authorization` header on its
 * own, and another site cannot set one without a CORS preflight this application refuses. Any other scheme still rides
 * on the session cookie. A malformed bearer token matches too, as the resource server refuses it with `401`.
 */
internal class BearerTokenRequests : RequestMatcher {
    private val bearerTokens: BearerTokenResolver = DefaultBearerTokenResolver()

    override fun matches(request: HttpServletRequest): Boolean =
        try {
            bearerTokens.resolve(request) != null
        } catch (_: OAuth2AuthenticationException) {
            true
        }
}
