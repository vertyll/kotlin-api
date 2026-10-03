package com.vertyll.kotlinapi.common.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.servlet.resource.NoResourceFoundException

class GlobalExceptionHandlerTest {
    private lateinit var handler: GlobalExceptionHandler

    @BeforeEach
    fun setUp() {
        handler = GlobalExceptionHandler()
    }

    private fun ProblemDetail.property(name: String): Any? = properties?.get(name)

    @Test
    fun handleApiException_ShouldReturnProblemWithKeyAndArgs() {
        val ex = ApiException("errors.role.notFound", HttpStatus.NOT_FOUND, mapOf("id" to 7))

        val problem = handler.handleApiException(ex)

        assertEquals(HttpStatus.NOT_FOUND.value(), problem.status)
        assertEquals("errors.role.notFound", problem.detail)
        assertEquals("errors.role.notFound", problem.property(Problems.CODE_PROPERTY))
        assertEquals(mapOf("id" to 7), problem.property(Problems.ARGS_PROPERTY))
    }

    @Test
    fun handleApiException_ShouldOmitEmptyArgs() {
        val problem = handler.handleApiException(ApiException("errors.user.notFound", HttpStatus.NOT_FOUND))

        assertNull(problem.property(Problems.ARGS_PROPERTY))
    }

    @Test
    fun handleValidationException_ShouldGroupKeysByField() {
        val ex = mock(MethodArgumentNotValidException::class.java)
        val bindingResult = mock(BindingResult::class.java)
        val errors =
            listOf(
                FieldError("object", "password", "validation.password.required"),
                FieldError("object", "password", "validation.password.tooShort"),
                FieldError("object", "email", "validation.email.invalid"),
            )
        `when`(ex.bindingResult).thenReturn(bindingResult)
        `when`(bindingResult.fieldErrors).thenReturn(errors)

        val problem = handler.handleValidationException(ex)

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.status)
        assertEquals(GlobalExceptionHandler.VALIDATION_FAILED, problem.property(Problems.CODE_PROPERTY))
        assertEquals(
            mapOf(
                "password" to listOf("validation.password.required", "validation.password.tooShort"),
                "email" to listOf("validation.email.invalid"),
            ),
            problem.property(Problems.ERRORS_PROPERTY),
        )
    }

    @Test
    fun accessDenied_ShouldMapToKey() {
        assertEquals(GlobalExceptionHandler.ACCESS_DENIED, handler.handleAccessDeniedException().detail)
        assertEquals(
            HttpStatus.FORBIDDEN.value(),
            handler.handleAccessDeniedException().status,
        )
    }

    @Test
    fun handleException_ShouldPassFrameworkProblemsThrough() {
        val problem = handler.handleException(NoResourceFoundException(HttpMethod.GET, "/nope", "nope"))

        assertEquals(HttpStatus.NOT_FOUND.value(), problem.status)
    }

    @Test
    fun handleException_ShouldHideUnexpectedErrors() {
        val problem = handler.handleException(RuntimeException("secret detail"))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.status)
        assertEquals(GlobalExceptionHandler.UNEXPECTED, problem.detail)
    }
}
