package com.vertyll.kotlinapi.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
    fun `the account takes its username from the token`() {
        val identity = KeycloakIdentity.from(token(mapOf("email" to "ada@kotlin-api.local", "preferred_username" to "ada")))

        assertEquals("ada", identity.username)
    }

    @Test
    fun `a token without a username is refused`() {
        assertThrows(IllegalArgumentException::class.java) {
            KeycloakIdentity.from(token(mapOf("email" to "ada@kotlin-api.local")))
        }
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
