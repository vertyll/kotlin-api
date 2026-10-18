package com.vertyll.kotlinapi.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.web.savedrequest.HttpSessionRequestCache
import org.springframework.security.web.savedrequest.RequestCache
import org.springframework.security.web.savedrequest.SimpleSavedRequest
import java.net.URI

/**
 * The page of the client a sign-in returns to, named in `return_to`. It is kept as Spring Security's saved request, on
 * the client's address, so Spring's own success handler returns there; only a path on the client's own site is kept,
 * so the sign-in cannot send the browser elsewhere.
 */
internal object ReturnPage {
    const val PARAMETER = "return_to"

    private val ATTRIBUTE: String = ReturnPage::class.java.name

    private const val MOST_LENGTH = 2048

    /** Spring Security's request cache, in the session, under the name this object saves the page under. */
    fun requestCache(): RequestCache = HttpSessionRequestCache().apply { setSessionAttrName(ATTRIBUTE) }

    /**
     * Saves the page this sign-in names, on the address of `start`, the client's start page, as the request it returns
     * to. A sign-in that names none forgets a page an earlier one named.
     */
    fun remember(
        request: HttpServletRequest,
        start: String,
    ) {
        val page = onThisSite(request.getParameter(PARAMETER))
        if (page != null) {
            request.session.setAttribute(ATTRIBUTE, SimpleSavedRequest(URI.create(start).resolve(page).toString()))
            return
        }
        request.getSession(false)?.takeIf { it.getAttribute(ATTRIBUTE) is SimpleSavedRequest }?.removeAttribute(ATTRIBUTE)
    }

    /** A path that starts with a single `/`, with no backslash and no control character. */
    fun onThisSite(path: String?): String? =
        path?.takeIf {
            it.startsWith("/") &&
                !it.startsWith("//") &&
                '\\' !in it &&
                it.none(Char::isISOControl) &&
                it.length <= MOST_LENGTH
        }
}
