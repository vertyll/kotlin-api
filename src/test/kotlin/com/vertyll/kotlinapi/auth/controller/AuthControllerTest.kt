package com.vertyll.kotlinapi.auth.controller

import com.vertyll.kotlinapi.auth.dto.AuthRequestDto
import com.vertyll.kotlinapi.auth.dto.AuthResponseDto
import com.vertyll.kotlinapi.auth.dto.ChangeEmailRequestDto
import com.vertyll.kotlinapi.auth.dto.ChangePasswordRequestDto
import com.vertyll.kotlinapi.auth.dto.RegisterRequestDto
import com.vertyll.kotlinapi.auth.dto.SessionResponseDto
import com.vertyll.kotlinapi.auth.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerTest {
    @Mock
    private lateinit var authService: AuthService

    @Mock
    private lateinit var request: HttpServletRequest

    @Mock
    private lateinit var response: HttpServletResponse

    @Mock
    private lateinit var securityContext: SecurityContext

    @Mock
    private lateinit var authentication: Authentication

    @InjectMocks
    private lateinit var authController: AuthController

    private val testEmail = "test@example.com"

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(securityContext)
        `when`(securityContext.authentication).thenReturn(authentication)
        `when`(authentication.name).thenReturn(testEmail)
    }

    @Test
    fun `register should call service and return success response`() {
        // given
        val registerRequest =
            RegisterRequestDto(
                firstName = "Test",
                lastName = "User",
                email = testEmail,
                password = "password123",
            )

        // when
        authController.register(registerRequest)

        // then
        verify(authService).register(registerRequest)
    }

    @Test
    fun `authenticate should call service and return auth response`() {
        // given
        val authRequest =
            AuthRequestDto(
                email = testEmail,
                password = "password123",
            )
        val authResponse =
            AuthResponseDto(
                token = "test-token",
                type = "Bearer",
            )
        `when`(authService.authenticate(authRequest, response)).thenReturn(authResponse)

        // when
        val result = authController.authenticate(authRequest, response)

        // then
        verify(authService).authenticate(authRequest, response)
        assertEquals(authResponse, result)
    }

    @Test
    fun `refreshToken should call service and return auth response`() {
        // given
        val authResponse =
            AuthResponseDto(
                token = "new-test-token",
                type = "Bearer",
            )
        `when`(authService.refreshToken(request, response)).thenReturn(authResponse)

        // when
        val result = authController.refreshToken(request, response)

        // then
        verify(authService).refreshToken(request, response)
        assertEquals(authResponse, result)
    }

    @Test
    fun `logout should call service and return success response`() {
        // when
        authController.logout(request, response)

        // then
        verify(authService).logout(request, response)
    }

    @Test
    fun `logoutAll should call service and return success response`() {
        // when
        authController.logoutAll(request, response)

        // then
        verify(authService).logoutAllSessions(request, response)
    }

    @Test
    fun `getSessions should get current user email and call service`() {
        // given
        val sessions =
            listOf(
                SessionResponseDto(id = 1L, deviceInfo = "Device 1", createdAt = LocalDateTime.of(2023, 1, 1, 0, 0)),
            )
        `when`(authService.getUserActiveSessions(testEmail)).thenReturn(sessions)

        // when
        val result = authController.getSessions()

        // then
        verify(authService).getUserActiveSessions(testEmail)
        assertEquals(sessions, result)
    }

    @Test
    fun `verifyAccount should call service and return success response`() {
        // given
        val code = "123456"

        // when
        authController.verifyAccount(code)

        // then
        verify(authService).verifyAccount(code)
    }

    @Test
    fun `requestEmailChange should call service and return success response`() {
        // given
        val changeEmailRequest =
            ChangeEmailRequestDto(
                currentPassword = "password123",
                newEmail = "new-email@example.com",
            )

        // when
        authController.requestEmailChange(changeEmailRequest)

        // then
        verify(authService).requestEmailChange(changeEmailRequest)
    }

    @Test
    fun `verifyEmailChange should call service and return auth response`() {
        // given
        val code = "123456"
        val authResponse =
            AuthResponseDto(
                token = "new-test-token",
                type = "Bearer",
            )
        `when`(authService.verifyEmailChange(code, response)).thenReturn(authResponse)

        // when
        val result = authController.verifyEmailChange(code, response)

        // then
        verify(authService).verifyEmailChange(code, response)
        assertEquals(authResponse, result)
    }

    @Test
    fun `requestPasswordChange should call service and return success response`() {
        // given
        val changePasswordRequest =
            ChangePasswordRequestDto(
                currentPassword = "password123",
                newPassword = "newpassword123",
            )

        // when
        authController.requestPasswordChange(changePasswordRequest)

        // then
        verify(authService).requestPasswordChange(changePasswordRequest)
    }

    @Test
    fun `verifyPasswordChange should call service and return success response`() {
        // given
        val code = "123456"

        // when
        authController.verifyPasswordChange(code)

        // then
        verify(authService).verifyPasswordChange(code)
    }
}
