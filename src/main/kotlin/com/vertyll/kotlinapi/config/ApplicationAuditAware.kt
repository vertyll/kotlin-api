package com.vertyll.kotlinapi.config

import org.springframework.data.domain.AuditorAware
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

class ApplicationAuditAware : AuditorAware<String> {
    override fun getCurrentAuditor(): Optional<String> {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication == null ||
            !authentication.isAuthenticated ||
            authentication is AnonymousAuthenticationToken
        ) {
            return Optional.of(SYSTEM_ACCOUNT)
        }

        return Optional
            .ofNullable(authentication.name)
            .filter { name -> name.isNotBlank() }
            .or { Optional.of(SYSTEM_ACCOUNT) }
    }

    private companion object {
        private const val SYSTEM_ACCOUNT = "SYSTEM"
    }
}
