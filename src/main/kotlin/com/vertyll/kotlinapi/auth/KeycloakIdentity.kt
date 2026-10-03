package com.vertyll.kotlinapi.auth

import org.springframework.security.oauth2.jwt.Jwt
import java.io.Serializable

data class KeycloakIdentity(
    val keycloakId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val roles: Set<String>,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
        private const val REALM_ACCESS = "realm_access"
        private const val ROLES = "roles"
        private const val DEFAULT_ROLES_PREFIX = "default-roles-"
        private val BUILT_IN_ROLES = setOf("offline_access", "uma_authorization")

        fun from(jwt: Jwt): KeycloakIdentity =
            KeycloakIdentity(
                keycloakId = requireNotNull(jwt.subject) { "The token has no subject" },
                email = requireNotNull(jwt.getClaimAsString("email")) { "The token has no email" },
                firstName = jwt.getClaimAsString("given_name").orEmpty(),
                lastName = jwt.getClaimAsString("family_name").orEmpty(),
                roles = realmRoles(jwt),
            )

        fun realmRoles(jwt: Jwt): Set<String> {
            val realmAccess = jwt.claims[REALM_ACCESS] as? Map<*, *> ?: return emptySet()
            val roles = realmAccess[ROLES] as? Collection<*> ?: return emptySet()
            return roles
                .filterIsInstance<String>()
                .filterNot { it in BUILT_IN_ROLES || it.startsWith(DEFAULT_ROLES_PREFIX) }
                .toSet()
        }
    }
}
