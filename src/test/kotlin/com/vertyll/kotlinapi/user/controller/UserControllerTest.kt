package com.vertyll.kotlinapi.user.controller

import com.vertyll.kotlinapi.auth.KeycloakIdentity
import com.vertyll.kotlinapi.user.dto.UserResponseDto
import com.vertyll.kotlinapi.user.service.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class UserControllerTest {
    private val dto = UserResponseDto(1L, "subject", "Ada", "Lovelace", "ada@kotlin-api.local", setOf("USER"))

    @Test
    fun `me synchronizes the account from the token`() {
        val service = mock<UserService> { on { sync(any()) } doReturn dto }
        val jwt =
            Jwt
                .withTokenValue("token")
                .header("alg", "RS256")
                .subject("subject")
                .claim("email", "ada@kotlin-api.local")
                .claim("given_name", "Ada")
                .claim("family_name", "Lovelace")
                .claim("realm_access", mapOf("roles" to listOf("USER")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build()

        assertEquals(dto, UserController(service).me(jwt))
        argumentCaptor<KeycloakIdentity>().apply {
            verify(service).sync(capture())
            assertEquals("ada@kotlin-api.local", firstValue.email)
            assertEquals(setOf("USER"), firstValue.roles)
        }
    }

    @Test
    fun `getUser delegates to the service`() {
        val service = mock<UserService> { on { getUserById(1L) } doReturn dto }

        assertEquals(dto, UserController(service).getUser(1L))
    }
}
