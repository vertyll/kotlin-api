package com.vertyll.kotlinapi.auth.service

import com.vertyll.kotlinapi.config.JwtProperties
import io.jsonwebtoken.ExpiredJwtException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mockito.doThrow
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.springframework.security.core.userdetails.UserDetails
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JwtServiceTest {
    private lateinit var jwtService: JwtService
    private lateinit var fixedClock: Clock

    private val secretKey =
        Base64.getEncoder().encodeToString(
            "my-very-strong-secret-key-1234567890abcd".toByteArray(),
        )
    private val accessTokenExpiration = 1000 * 60 * 60 // 1 hour
    private val refreshTokenExpiration = 1000 * 60 * 60 * 24 * 7 // 7 days
    private val refreshTokenCookieName = "refresh-token"

    private val mockUserDetails: UserDetails =
        mock {
            on { username } doReturn "testuser"
        }

    @BeforeEach
    fun setUp() {
        fixedClock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC)
        jwtService = JwtService(properties(accessTokenExpiration.toLong()), fixedClock)
    }

    private fun properties(accessExpiration: Long) =
        JwtProperties(
            secretKey = secretKey,
            accessTokenExpiration = accessExpiration,
            refreshTokenExpiration = refreshTokenExpiration.toLong(),
            refreshTokenCookieName = refreshTokenCookieName,
        )

    @Test
    fun `should generate valid access token and extract username`() {
        val token = jwtService.generateToken(mockUserDetails)
        val username = jwtService.extractUsername(token)

        assertEquals("testuser", username)
    }

    @Test
    fun `should generate refresh token with correct expiration`() {
        val token = jwtService.generateRefreshToken(mockUserDetails)
        val username = jwtService.extractUsername(token)

        assertEquals("testuser", username)
    }

    @Test
    fun `isTokenValid should return true for valid token`() {
        val token = jwtService.generateToken(mockUserDetails)
        val result = jwtService.isTokenValid(token, mockUserDetails)

        assertTrue(result)
    }

    @Test
    fun `isTokenValid should return false for expired token`() {
        val pastClock =
            Clock.fixed(
                Instant.parse("2024-12-31T00:00:00Z"),
                ZoneOffset.UTC,
            )
        val expiredJwtService = JwtService(properties(1000L), pastClock)

        val expiredToken = expiredJwtService.generateToken(mockUserDetails)

        val result = jwtService.isTokenValid(expiredToken, mockUserDetails)
        assertFalse(result)
    }

    @Test
    fun `isTokenValid should return false when token is expired with exception`() {
        val spyService = spy(jwtService)
        val expiredToken = "expired.jwt.token"

        doThrow(ExpiredJwtException(null, null, "Expired"))
            .whenever(spyService)
            .extractUsername(expiredToken)

        val result = spyService.isTokenValid(expiredToken, mockUserDetails)
        assertFalse(result)
    }

    @Test
    fun `should return refresh token cookie name`() {
        assertEquals(refreshTokenCookieName, jwtService.getRefreshTokenCookieName())
    }

    @Test
    fun `should return refresh token expiration time`() {
        assertEquals(refreshTokenExpiration.toLong(), jwtService.getRefreshTokenExpirationTime())
    }

    @Test
    fun `should not throw when extracting claim`() {
        val token = jwtService.generateToken(mockUserDetails)
        assertDoesNotThrow {
            val claim = jwtService.extractClaim(token) { it.subject }
            assertEquals("testuser", claim)
        }
    }
}
