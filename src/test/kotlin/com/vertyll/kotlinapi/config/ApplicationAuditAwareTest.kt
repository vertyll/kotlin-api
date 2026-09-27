package com.vertyll.kotlinapi.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder

class ApplicationAuditAwareTest {
    private val auditAware = ApplicationAuditAware()

    @Test
    fun getCurrentAuditor_WhenAuthenticated_ShouldReturnUsername() {
        val authentication = mock(Authentication::class.java)
        val securityContext = mock(SecurityContext::class.java)
        SecurityContextHolder.setContext(securityContext)

        `when`(securityContext.authentication).thenReturn(authentication)
        `when`(authentication.isAuthenticated).thenReturn(true)
        `when`(authentication.name).thenReturn("testUser")

        val result = auditAware.currentAuditor

        assertTrue(result.isPresent)
        assertEquals("testUser", result.get())
    }

    @Test
    fun getCurrentAuditor_WhenNotAuthenticated_ShouldReturnSystem() {
        val authentication = mock(Authentication::class.java)
        val securityContext = mock(SecurityContext::class.java)
        SecurityContextHolder.setContext(securityContext)

        `when`(securityContext.authentication).thenReturn(authentication)
        `when`(authentication.isAuthenticated).thenReturn(false)

        val result = auditAware.currentAuditor

        assertTrue(result.isPresent)
        assertEquals("SYSTEM", result.get())
    }

    @Test
    fun getCurrentAuditor_WhenNoAuthentication_ShouldReturnSystem() {
        val securityContext = mock(SecurityContext::class.java)
        SecurityContextHolder.setContext(securityContext)
        `when`(securityContext.authentication).thenReturn(null)

        val result = auditAware.currentAuditor

        assertTrue(result.isPresent)
        assertEquals("SYSTEM", result.get())
    }
}
