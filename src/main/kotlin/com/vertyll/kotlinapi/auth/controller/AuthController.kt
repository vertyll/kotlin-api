package com.vertyll.kotlinapi.auth.controller

import com.vertyll.kotlinapi.auth.dto.AuthRequestDto
import com.vertyll.kotlinapi.auth.dto.AuthResponseDto
import com.vertyll.kotlinapi.auth.dto.ChangeEmailRequestDto
import com.vertyll.kotlinapi.auth.dto.ChangePasswordRequestDto
import com.vertyll.kotlinapi.auth.dto.RegisterRequestDto
import com.vertyll.kotlinapi.auth.dto.ResetPasswordRequestDto
import com.vertyll.kotlinapi.auth.dto.SessionResponseDto
import com.vertyll.kotlinapi.auth.service.AuthService
import com.vertyll.kotlinapi.common.exception.ApiException
import com.vertyll.kotlinapi.config.ProblemSecurityHandlers
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.mail.MessagingException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Auth management APIs")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Register new user")
    @Throws(MessagingException::class)
    fun register(
        @RequestBody @Valid request: RegisterRequestDto,
    ) = authService.register(request)

    @PostMapping("/authenticate")
    @Operation(summary = "Authenticate user and get token")
    fun authenticate(
        @RequestBody @Valid request: AuthRequestDto,
        response: HttpServletResponse,
    ): AuthResponseDto = authService.authenticate(request, response)

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token using refresh token cookie")
    fun refreshToken(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): AuthResponseDto = authService.refreshToken(request, response)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout from current session")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) = authService.logout(request, response)

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout from all sessions")
    fun logoutAll(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) = authService.logoutAllSessions(request, response)

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all active sessions for the current user")
    fun getSessions(): List<SessionResponseDto> {
        val authentication =
            SecurityContextHolder.getContext().authentication
                ?: throw ApiException(ProblemSecurityHandlers.AUTHENTICATION_REQUIRED, HttpStatus.UNAUTHORIZED)
        return authService.getUserActiveSessions(authentication.name)
    }

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Verify user account with code")
    fun verifyAccount(
        @RequestParam code: String,
    ) = authService.verifyAccount(code)

    @PostMapping("/change-email-request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Request email change, sends verification to new email")
    @Throws(MessagingException::class)
    fun requestEmailChange(
        @RequestBody @Valid request: ChangeEmailRequestDto,
    ) = authService.requestEmailChange(request)

    @PostMapping("/verify-email-change")
    @Operation(summary = "Verify email change with code")
    fun verifyEmailChange(
        @RequestParam code: String,
        response: HttpServletResponse,
    ): AuthResponseDto = authService.verifyEmailChange(code, response)

    @PostMapping("/change-password-request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Request password change, sends verification email")
    @Throws(MessagingException::class)
    fun requestPasswordChange(
        @RequestBody @Valid request: ChangePasswordRequestDto,
    ) = authService.requestPasswordChange(request)

    @PostMapping("/verify-password-change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Verify password change with code")
    fun verifyPasswordChange(
        @RequestParam code: String,
    ) = authService.verifyPasswordChange(code)

    @PostMapping("/reset-password-request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Request password reset for a forgotten password")
    @Throws(MessagingException::class)
    fun requestPasswordReset(
        @RequestParam email: String,
    ) = authService.sendPasswordResetEmail(email)

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reset password using reset token")
    fun resetPassword(
        @RequestParam token: String,
        @RequestBody @Valid request: ResetPasswordRequestDto,
    ) = authService.resetPassword(token, request)
}
