package com.vertyll.kotlinapi.auth

import jakarta.servlet.http.HttpServletRequest

object FetchMetadata {
    private const val FETCH_SITE_HEADER = "Sec-Fetch-Site"
    private val SAFE_METHODS = setOf("GET", "HEAD", "OPTIONS")
    private val TRUSTED_FETCH_SITES = setOf("same-origin", "none")

    fun sentFromThisOrigin(request: HttpServletRequest): Boolean =
        request.method in SAFE_METHODS || request.getHeader(FETCH_SITE_HEADER).let { it == null || it in TRUSTED_FETCH_SITES }
}
