package com.vertyll.kotlinapi.auth.service

import com.vertyll.kotlinapi.auth.model.RefreshToken
import com.vertyll.kotlinapi.auth.repository.RefreshTokenRepository
import com.vertyll.kotlinapi.common.exception.ApiException
import com.vertyll.kotlinapi.user.model.User
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun createRefreshToken(
        user: User,
        deviceInfo: String? = null,
    ): String {
        val tokenValue = UUID.randomUUID().toString()

        val hashedToken = requireNotNull(passwordEncoder.encode(tokenValue)) { "Token encoding failed" }

        val refreshToken =
            RefreshToken(
                token = hashedToken,
                user = user,
                expiryDate = Instant.now().plusMillis(jwtService.getRefreshTokenExpirationTime()),
                revoked = false,
                deviceInfo = deviceInfo,
            )

        refreshTokenRepository.save(refreshToken)

        return tokenValue
    }

    @Transactional(readOnly = true)
    fun validateRefreshToken(token: String): User {
        val allTokens =
            refreshTokenRepository
                .findAll()
                .filter { !it.revoked && it.expiryDate.isAfter(Instant.now()) }

        val refreshToken =
            allTokens.find { passwordEncoder.matches(token, it.token) }
                ?: throw ApiException("errors.auth.invalidRefreshToken", HttpStatus.UNAUTHORIZED)

        return refreshToken.user
    }

    @Transactional
    fun rotateRefreshToken(
        oldToken: String,
        deviceInfo: String? = null,
    ): String {
        val allTokens =
            refreshTokenRepository
                .findAll()
                .filter { !it.revoked && it.expiryDate.isAfter(Instant.now()) }

        val refreshToken =
            allTokens.find { passwordEncoder.matches(oldToken, it.token) }
                ?: throw ApiException("errors.auth.invalidRefreshToken", HttpStatus.UNAUTHORIZED)

        refreshToken.revoked = true
        refreshTokenRepository.save(refreshToken)

        return createRefreshToken(refreshToken.user, deviceInfo)
    }

    @Transactional
    fun revokeRefreshToken(token: String) {
        val allTokens =
            refreshTokenRepository
                .findAll()
                .filter { !it.revoked && it.expiryDate.isAfter(Instant.now()) }

        val refreshToken =
            allTokens.find { passwordEncoder.matches(token, it.token) }
                ?: return // Token not found or already revoked, nothing to do

        refreshToken.revoked = true
        refreshTokenRepository.save(refreshToken)
    }

    @Transactional
    fun revokeAllUserTokens(user: User) {
        refreshTokenRepository.revokeAllUserTokens(user)
    }

    @Transactional(readOnly = true)
    fun getUserActiveSessions(user: User): List<RefreshToken> =
        refreshTokenRepository
            .findByUserAndRevoked(user, false)
            .filter { it.expiryDate.isAfter(Instant.now()) }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    fun cleanupExpiredTokens() {
        refreshTokenRepository.deleteAllExpiredTokens(Instant.now())
    }
}
