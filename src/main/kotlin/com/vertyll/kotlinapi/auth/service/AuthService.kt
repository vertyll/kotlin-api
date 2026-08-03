package com.vertyll.kotlinapi.auth.service

import com.vertyll.kotlinapi.auth.dto.AuthRequestDto
import com.vertyll.kotlinapi.auth.dto.AuthResponseDto
import com.vertyll.kotlinapi.auth.dto.ChangeEmailRequestDto
import com.vertyll.kotlinapi.auth.dto.ChangePasswordRequestDto
import com.vertyll.kotlinapi.auth.dto.RegisterRequestDto
import com.vertyll.kotlinapi.auth.dto.ResetPasswordRequestDto
import com.vertyll.kotlinapi.auth.enums.VerificationTokenType
import com.vertyll.kotlinapi.auth.model.VerificationToken
import com.vertyll.kotlinapi.auth.repository.VerificationTokenRepository
import com.vertyll.kotlinapi.common.exception.ApiException
import com.vertyll.kotlinapi.email.enums.EmailTemplateName
import com.vertyll.kotlinapi.email.service.IEmailService
import com.vertyll.kotlinapi.role.service.RoleService
import com.vertyll.kotlinapi.user.model.User
import com.vertyll.kotlinapi.user.repository.UserRepository
import jakarta.mail.MessagingException
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val tokenRepository: VerificationTokenRepository,
    private val roleService: RoleService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService,
    private val authenticationManager: AuthenticationManager,
    private val emailService: IEmailService,
) {
    private companion object {
        private const val ERROR_PASSWORD_ENCODING_FAILED = "Password encoding failed"
        private const val ERROR_USER_NOT_FOUND = "User not found"
        private const val ERROR_VERIFICATION_CODE_ALREADY_USED = "Verification code already used"
        private const val ERROR_VERIFICATION_CODE_EXPIRED = "Verification code expired"
        private const val ERROR_INVALID_VERIFICATION_CODE_TYPE = "Invalid verification code type"
        private const val VERIFICATION_CODE_MIN = 100000
        private const val VERIFICATION_CODE_RANGE = 900000
        private const val VERIFICATION_TOKEN_EXPIRY_HOURS = 24L
        private const val MILLIS_IN_SECOND = 1000

        private val secureRandom = SecureRandom()
    }

    @Transactional
    @Throws(MessagingException::class)
    fun register(request: RegisterRequestDto) {
        if (userRepository.existsByEmail(request.email)) {
            throw ApiException("Email already registered", HttpStatus.BAD_REQUEST)
        }

        val user =
            User.create(
                firstName = request.firstName,
                lastName = request.lastName,
                email = request.email,
                password = requireNotNull(passwordEncoder.encode(request.password)) { ERROR_PASSWORD_ENCODING_FAILED },
                roles = setOf(roleService.getOrCreateDefaultRole("USER")).toMutableSet(),
                enabled = false,
            )

        userRepository.save(user)

        val verificationCode = generateVerificationCode()
        createVerificationToken(
            user = user,
            token = verificationCode,
            tokenType = VerificationTokenType.ACCOUNT_ACTIVATION,
        )
        emailService.sendEmail(
            user.username,
            user.firstName,
            EmailTemplateName.ACTIVATE_ACCOUNT,
            verificationCode,
            "Account activation",
        )
    }

    @Transactional
    fun authenticate(
        request: AuthRequestDto,
        response: HttpServletResponse? = null,
    ): AuthResponseDto {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                request.email,
                request.password,
            ),
        )

        val user =
            userRepository
                .findByEmailWithRoles(request.email)
                .orElseThrow { ApiException(ERROR_USER_NOT_FOUND, HttpStatus.NOT_FOUND) }

        if (!user.isEnabled) {
            throw ApiException("Account not verified", HttpStatus.FORBIDDEN)
        }

        val jwtToken = jwtService.generateToken(user)

        // Create refresh token and set as HTTP-only cookie if response is provided
        response?.let {
            val refreshToken = refreshTokenService.createRefreshToken(user, request.deviceInfo)
            addRefreshTokenCookie(it, refreshToken)
        }

        return AuthResponseDto(
            token = jwtToken,
            type = "Bearer",
        )
    }

    @Transactional
    fun refreshToken(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): AuthResponseDto {
        val refreshToken =
            extractRefreshTokenFromCookies(request)
                ?: throw ApiException("Refresh token not found", HttpStatus.UNAUTHORIZED)

        val user = refreshTokenService.validateRefreshToken(refreshToken)

        val accessToken = jwtService.generateToken(user)

        val newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken)

        // Set new refresh token as cookie
        addRefreshTokenCookie(response, newRefreshToken)

        return AuthResponseDto(
            token = accessToken,
            type = "Bearer",
        )
    }

    @Transactional
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val refreshToken = extractRefreshTokenFromCookies(request)

        refreshToken?.let {
            refreshTokenService.revokeRefreshToken(it)
        }

        // Clear the refresh token cookie
        deleteRefreshTokenCookie(response)
    }

    @Transactional
    fun logoutAllSessions(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val refreshToken = extractRefreshTokenFromCookies(request)

        if (refreshToken != null) {
            val user = refreshTokenService.validateRefreshToken(refreshToken)

            refreshTokenService.revokeAllUserTokens(user)

            // Clear the refresh token cookie
            deleteRefreshTokenCookie(response)
        } else {
            throw ApiException("Refresh token not found", HttpStatus.UNAUTHORIZED)
        }
    }

    @Transactional
    fun getUserActiveSessions(email: String): List<Map<String, Any>> {
        val user =
            userRepository
                .findByEmailWithRoles(email)
                .orElseThrow { ApiException(ERROR_USER_NOT_FOUND, HttpStatus.NOT_FOUND) }

        return refreshTokenService
            .getUserActiveSessions(user)
            .map { token ->
                mapOf<String, Any>(
                    "id" to (token.id ?: 0L),
                    "deviceInfo" to (token.deviceInfo ?: "Unknown device"),
                    "createdAt" to (token.createdAt ?: ""),
                )
            }
    }

    @Transactional
    fun verifyAccount(code: String) {
        val verificationToken = getVerificationTokenByCode(code)

        if (verificationToken.used) {
            throw ApiException(ERROR_VERIFICATION_CODE_ALREADY_USED, HttpStatus.BAD_REQUEST)
        }

        if (verificationToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw ApiException(ERROR_VERIFICATION_CODE_EXPIRED, HttpStatus.BAD_REQUEST)
        }

        if (verificationToken.tokenType != VerificationTokenType.ACCOUNT_ACTIVATION) {
            throw ApiException(ERROR_INVALID_VERIFICATION_CODE_TYPE, HttpStatus.BAD_REQUEST)
        }

        val user = verificationToken.user ?: throw ApiException(ERROR_USER_NOT_FOUND, HttpStatus.NOT_FOUND)
        user.enabled = true
        verificationToken.used = true

        userRepository.save(user)
        tokenRepository.save(verificationToken)
    }

    private fun generateVerificationCode(): String {
        val code = VERIFICATION_CODE_MIN + secureRandom.nextInt(VERIFICATION_CODE_RANGE)
        return code.toString()
    }

    private fun createVerificationToken(
        user: User,
        token: String,
        tokenType: VerificationTokenType = VerificationTokenType.ACCOUNT_ACTIVATION,
        additionalData: String? = null,
    ) {
        val verificationToken =
            VerificationToken(
                token = token,
                user = user,
                expiryDate = LocalDateTime.now().plusHours(VERIFICATION_TOKEN_EXPIRY_HOURS),
                used = false,
                tokenType = tokenType,
                additionalData = additionalData,
            )

        tokenRepository.save(verificationToken)
    }

    private fun addRefreshTokenCookie(
        response: HttpServletResponse,
        token: String,
    ) {
        val cookie = Cookie(jwtService.getRefreshTokenCookieName(), token)
        cookie.isHttpOnly = true
        cookie.secure = true
        cookie.path = "/"
        cookie.maxAge = (jwtService.getRefreshTokenExpirationTime() / MILLIS_IN_SECOND).toInt()
        response.addCookie(cookie)
    }

    private fun deleteRefreshTokenCookie(response: HttpServletResponse) {
        val cookie = Cookie(jwtService.getRefreshTokenCookieName(), "")
        cookie.isHttpOnly = true
        cookie.secure = true
        cookie.path = "/"
        cookie.maxAge = 0
        response.addCookie(cookie)
    }

    private fun extractRefreshTokenFromCookies(request: HttpServletRequest): String? {
        val cookies = request.cookies ?: return null

        return cookies.find { it.name == jwtService.getRefreshTokenCookieName() }?.value
    }

    @Transactional
    @Throws(MessagingException::class)
    fun requestEmailChange(request: ChangeEmailRequestDto) {
        val authentication = getCurrentAuthentication()
        val currentEmail = authentication.name

        val user =
            userRepository
                .findByEmailWithRoles(currentEmail)
                .orElseThrow { ApiException(ERROR_USER_NOT_FOUND, HttpStatus.NOT_FOUND) }

        if (!passwordEncoder.matches(request.currentPassword, user.password)) {
            throw ApiException("Invalid current password", HttpStatus.BAD_REQUEST)
        }

        if (userRepository.existsByEmail(request.newEmail)) {
            throw ApiException("Email already in use", HttpStatus.BAD_REQUEST)
        }

        val verificationCode = generateVerificationCode()

        createVerificationToken(
            user = user,
            token = verificationCode,
            tokenType = VerificationTokenType.EMAIL_CHANGE,
            additionalData = request.newEmail,
        )

        emailService.sendEmail(
            to = request.newEmail,
            username = user.firstName,
            emailTemplate = EmailTemplateName.CHANGE_EMAIL,
            activationCode = verificationCode,
            subject = "Email Change Verification",
        )
    }

    @Transactional
    fun verifyEmailChange(
        code: String,
        response: HttpServletResponse,
    ): AuthResponseDto {
        val verificationToken = getVerificationTokenByCode(code)

        if (verificationToken.used) {
            throw ApiException(ERROR_VERIFICATION_CODE_ALREADY_USED, HttpStatus.BAD_REQUEST)
        }

        if (verificationToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw ApiException(ERROR_VERIFICATION_CODE_EXPIRED, HttpStatus.BAD_REQUEST)
        }

        if (verificationToken.tokenType != VerificationTokenType.EMAIL_CHANGE) {
            throw ApiException(ERROR_INVALID_VERIFICATION_CODE_TYPE, HttpStatus.BAD_REQUEST)
        }

        val user = verificationToken.user ?: throw ApiException(ERROR_USER_NOT_FOUND, HttpStatus.NOT_FOUND)
        val newEmail = verificationToken.additionalData ?: throw ApiException("New email not found", HttpStatus.BAD_REQUEST)

        val updatedUser = createUpdatedUser(user, email = newEmail)
        updatedUser.id = user.id

        userRepository.save(updatedUser)

        verificationToken.used = true
        tokenRepository.save(verificationToken)

        refreshTokenService.revokeAllUserTokens(user)

        val jwtToken = jwtService.generateToken(updatedUser)

        val refreshToken = refreshTokenService.createRefreshToken(updatedUser)
        addRefreshTokenCookie(response, refreshToken)

        return AuthResponseDto(
            token = jwtToken,
            type = "Bearer",
        )
    }

    @Transactional
    @Throws(MessagingException::class)
    fun requestPasswordChange(request: ChangePasswordRequestDto) {
        val authentication = getCurrentAuthentication()
        val email = authentication.name

        val user =
            userRepository
                .findByEmailWithRoles(email)
                .orElseThrow { ApiException(ERROR_USER_NOT_FOUND, HttpStatus.NOT_FOUND) }

        if (!passwordEncoder.matches(request.currentPassword, user.password)) {
            throw ApiException("Invalid current password", HttpStatus.BAD_REQUEST)
        }

        val verificationCode = generateVerificationCode()

        createVerificationToken(
            user = user,
            token = verificationCode,
            tokenType = VerificationTokenType.PASSWORD_CHANGE,
            additionalData = requireNotNull(passwordEncoder.encode(request.newPassword)) { ERROR_PASSWORD_ENCODING_FAILED },
        )

        emailService.sendEmail(
            to = user.username,
            username = user.firstName,
            emailTemplate = EmailTemplateName.CHANGE_PASSWORD,
            activationCode = verificationCode,
            subject = "Password Change Verification",
        )
    }

    @Transactional
    fun verifyPasswordChange(code: String) {
        val verificationToken = getVerificationTokenByCode(code)

        if (verificationToken.used) {
            throw ApiException(ERROR_VERIFICATION_CODE_ALREADY_USED, HttpStatus.BAD_REQUEST)
        }

        if (verificationToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw ApiException(ERROR_VERIFICATION_CODE_EXPIRED, HttpStatus.BAD_REQUEST)
        }

        if (verificationToken.tokenType != VerificationTokenType.PASSWORD_CHANGE) {
            throw ApiException(ERROR_INVALID_VERIFICATION_CODE_TYPE, HttpStatus.BAD_REQUEST)
        }

        val user = verificationToken.user ?: throw ApiException(ERROR_USER_NOT_FOUND, HttpStatus.NOT_FOUND)
        val newPasswordHash = verificationToken.additionalData ?: throw ApiException("New password not found", HttpStatus.BAD_REQUEST)

        val updatedUser = createUpdatedUser(user, password = newPasswordHash)

        userRepository.save(updatedUser)

        verificationToken.used = true
        tokenRepository.save(verificationToken)

        refreshTokenService.revokeAllUserTokens(user)
    }

    @Transactional
    @Throws(MessagingException::class)
    fun sendPasswordResetEmail(email: String) {
        val user =
            userRepository
                .findByEmailWithRoles(email)
                .orElseThrow { ApiException(ERROR_USER_NOT_FOUND, HttpStatus.NOT_FOUND) }

        val verificationCode = generateVerificationCode()

        createVerificationToken(
            user = user,
            token = verificationCode,
            tokenType = VerificationTokenType.PASSWORD_RESET,
        )

        emailService.sendEmail(
            to = email,
            username = user.firstName,
            emailTemplate = EmailTemplateName.RESET_PASSWORD,
            activationCode = verificationCode,
            subject = "Password Reset",
        )
    }

    @Transactional
    fun resetPassword(
        token: String,
        request: ResetPasswordRequestDto,
    ) {
        val verificationToken = getVerificationTokenByCode(token)

        if (verificationToken.used) {
            throw ApiException("Verification token already used", HttpStatus.BAD_REQUEST)
        }

        if (verificationToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw ApiException("Verification token expired", HttpStatus.BAD_REQUEST)
        }

        if (verificationToken.tokenType != VerificationTokenType.PASSWORD_RESET) {
            throw ApiException("Invalid verification token type", HttpStatus.BAD_REQUEST)
        }

        val user = verificationToken.user ?: throw ApiException(ERROR_USER_NOT_FOUND, HttpStatus.NOT_FOUND)

        val newPasswordHash = requireNotNull(passwordEncoder.encode(request.newPassword)) { ERROR_PASSWORD_ENCODING_FAILED }

        val updatedUser = createUpdatedUser(user, password = newPasswordHash)

        userRepository.save(updatedUser)

        verificationToken.used = true
        tokenRepository.save(verificationToken)

        refreshTokenService.revokeAllUserTokens(user)
    }

    /**
     * Gets a verification token by its code.
     * Throws an ApiException if the token is not found.
     *
     * @param code The verification code
     * @return The verification token
     * @throws ApiException If the verification code is invalid
     */
    private fun getVerificationTokenByCode(code: String): VerificationToken =
        tokenRepository
            .findByToken(code)
            .orElseThrow { ApiException("Invalid verification code", HttpStatus.BAD_REQUEST) }

    /**
     * Gets the current authentication from the security context.
     *
     * @return The current authentication
     */
    private fun getCurrentAuthentication(): Authentication =
        SecurityContextHolder.getContext().authentication
            ?: throw ApiException("Unauthorized", HttpStatus.UNAUTHORIZED)

    /**
     * Creates an updated user instance while preserving the original ID.
     *
     * @param user The original user
     * @param email Optional new email (defaults to original user's username)
     * @param password Optional new password (defaults to original user's password)
     * @return A new User instance with updated fields but same ID
     */
    private fun createUpdatedUser(
        user: User,
        email: String = user.username,
        password: String = user.password,
    ): User {
        val updatedUser =
            User.create(
                firstName = user.firstName,
                lastName = user.lastName,
                email = email,
                password = password,
                roles = user.roles,
                enabled = user.isEnabled,
            )

        updatedUser.id = user.id
        return updatedUser
    }
}
