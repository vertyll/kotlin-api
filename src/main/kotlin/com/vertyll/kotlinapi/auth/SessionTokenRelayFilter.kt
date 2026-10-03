package com.vertyll.kotlinapi.auth

import com.vertyll.kotlinapi.common.exception.ApiException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Clock
import java.time.Duration
import java.util.Collections
import java.util.Enumeration

class SessionTokenRelayFilter(
    private val browserSessions: BrowserSessions,
    private val sessions: SessionService,
    private val clock: Clock,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(SessionTokenRelayFilter::class.java)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI.startsWith(request.contextPath + AUTH_PATH) ||
            request.getHeader(HttpHeaders.AUTHORIZATION) != null ||
            !sentBySameOrigin(request)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val session = freshen(request)
        chain.doFilter(if (session == null) request else BearerRequest(request, session.accessToken), response)
    }

    private fun freshen(request: HttpServletRequest): AuthSession? {
        val current = browserSessions.current(request) ?: return null
        if (!current.needsRefreshAt(clock.instant(), REFRESH_SKEW)) {
            return current
        }
        return try {
            sessions.refresh(current).also { browserSessions.replace(request, it) }
        } catch (e: ApiException) {
            if (e.messageKey == AuthErrors.SESSION_EXPIRED) {
                browserSessions.end(request)
            } else {
                log.warn("Could not refresh the session of {}: {}", current.identity.keycloakId, e.messageKey)
            }
            null
        }
    }

    private fun sentBySameOrigin(request: HttpServletRequest): Boolean =
        request.method in SAFE_METHODS || request.getHeader(FETCH_SITE_HEADER).let { it == null || it in TRUSTED_FETCH_SITES }

    private class BearerRequest(
        request: HttpServletRequest,
        accessToken: String,
    ) : HttpServletRequestWrapper(request) {
        private val authorization = "$BEARER_PREFIX$accessToken"

        override fun getHeader(name: String): String? = if (isAuthorization(name)) authorization else super.getHeader(name)

        override fun getHeaders(name: String): Enumeration<String> =
            if (isAuthorization(name)) Collections.enumeration(listOf(authorization)) else super.getHeaders(name)

        override fun getHeaderNames(): Enumeration<String> =
            Collections.enumeration(Collections.list(super.getHeaderNames()).toSet() + HttpHeaders.AUTHORIZATION)

        private fun isAuthorization(name: String): Boolean = HttpHeaders.AUTHORIZATION.equals(name, ignoreCase = true)
    }

    private companion object {
        private const val AUTH_PATH = "/auth/"
        private const val BEARER_PREFIX = "Bearer "
        private const val FETCH_SITE_HEADER = "Sec-Fetch-Site"
        private val REFRESH_SKEW = Duration.ofSeconds(30)
        private val SAFE_METHODS = setOf("GET", "HEAD", "OPTIONS")
        private val TRUSTED_FETCH_SITES = setOf("same-origin", "none")
    }
}
