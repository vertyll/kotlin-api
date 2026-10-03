package com.vertyll.kotlinapi.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class KeycloakIdentityTest {
    @Test
    fun `realm roles leave out what Keycloak grants every account`() {
        val jwt =
            token(
                mapOf(
                    "realm_access" to mapOf("roles" to listOf("ADMIN", "offline_access", "uma_authorization", "default-roles-kotlin-api")),
                ),
            )

        assertEquals(setOf("ADMIN"), KeycloakIdentity.realmRoles(jwt))
    }

    @Test
    fun `a token without realm access carries no roles`() {
        assertEquals(emptySet<String>(), KeycloakIdentity.realmRoles(token(emptyMap())))
    }

    @Test
    fun `missing names become empty rather than failing the sign-in`() {
        val identity = KeycloakIdentity.from(token(mapOf("email" to "ada@kotlin-api.local")))

        assertEquals("", identity.firstName)
        assertEquals("ada@kotlin-api.local", identity.email)
    }

    private fun token(claims: Map<String, Any>): Jwt =
        Jwt
            .withTokenValue("token")
            .header("alg", "RS256")
            .subject("subject")
            .claims { it.putAll(claims) }
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build()
}
