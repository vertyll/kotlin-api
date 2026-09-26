package com.vertyll.kotlinapi.auth.service

import com.vertyll.kotlinapi.config.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.security.Key
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    private val properties: JwtProperties,
    private val clock: Clock,
) {
    fun extractUsername(token: String): String = extractClaim(token) { it.subject }

    fun generateToken(userDetails: UserDetails): String = generateToken(emptyMap(), userDetails)

    fun generateToken(
        extraClaims: Map<String, Any>,
        userDetails: UserDetails,
    ): String {
        val now = Instant.now(clock)
        return Jwts
            .builder()
            .claims(extraClaims)
            .subject(userDetails.username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(properties.accessTokenExpiration, ChronoUnit.MILLIS)))
            .signWith(getSigningKey())
            .compact()
    }

    fun generateRefreshToken(userDetails: UserDetails): String = generateRefreshToken(emptyMap(), userDetails)

    fun generateRefreshToken(
        extraClaims: Map<String, Any>,
        userDetails: UserDetails,
    ): String {
        val now = Instant.now(clock)
        return Jwts
            .builder()
            .claims(extraClaims)
            .subject(userDetails.username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(properties.refreshTokenExpiration, ChronoUnit.MILLIS)))
            .signWith(getSigningKey())
            .compact()
    }

    fun getRefreshTokenCookieName(): String = properties.refreshTokenCookieName

    fun getRefreshTokenExpirationTime(): Long = properties.refreshTokenExpiration

    fun isTokenValid(
        token: String,
        userDetails: UserDetails,
    ): Boolean =
        try {
            val username = extractUsername(token)
            username == userDetails.username && !isTokenExpired(token)
        } catch (_: Exception) {
            false
        }

    private fun isTokenExpired(token: String): Boolean = extractExpiration(token).before(Date.from(Instant.now(clock)))

    private fun extractExpiration(token: String): Date = extractClaim(token) { it.expiration }

    fun <T> extractClaim(
        token: String,
        claimsResolver: (Claims) -> T,
    ): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    private fun extractAllClaims(token: String): Claims =
        try {
            Jwts
                .parser()
                .verifyWith(getVerificationKey())
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            e.claims
        }

    private fun getSigningKey(): Key {
        val keyBytes = Decoders.BASE64.decode(properties.secretKey)
        return Keys.hmacShaKeyFor(keyBytes)
    }

    private fun getVerificationKey(): SecretKey {
        val keyBytes = Decoders.BASE64.decode(properties.secretKey)
        return Keys.hmacShaKeyFor(keyBytes)
    }
}
