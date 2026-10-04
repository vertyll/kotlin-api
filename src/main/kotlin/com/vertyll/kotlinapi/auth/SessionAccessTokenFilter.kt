package com.vertyll.kotlinapi.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.filter.OncePerRequestFilter

class SessionAccessTokenFilter(
    private val accessTokens: SessionAccessTokens,
    private val tokenAuthentication: Converter<Jwt, AbstractAuthenticationToken>,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val session = SecurityContextHolder.getContext().authentication as? OAuth2AuthenticationToken
        if (session != null) {
            val context = SecurityContextHolder.createEmptyContext()
            if (FetchMetadata.sentFromThisOrigin(request)) {
                accessTokens.current(session, request, response)?.let { context.authentication = tokenAuthentication.convert(it) }
            }
            SecurityContextHolder.setContext(context)
        }
        chain.doFilter(request, response)
    }
}
