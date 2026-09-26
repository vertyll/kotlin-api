package com.vertyll.kotlinapi.common.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ApiExceptionTest {
    @Test
    fun constructor_ShouldCarryKeyStatusAndArgs() {
        // when
        val exception = ApiException("errors.role.notFound", HttpStatus.NOT_FOUND, mapOf("id" to 7))

        // then
        assertEquals("errors.role.notFound", exception.messageKey)
        assertEquals("errors.role.notFound", exception.message)
        assertEquals(HttpStatus.NOT_FOUND, exception.status)
        assertEquals(mapOf("id" to 7), exception.args)
    }

    @Test
    fun args_ShouldDefaultToEmpty() {
        // when
        val exception = ApiException("errors.user.notFound", HttpStatus.NOT_FOUND)

        // then
        assertEquals(emptyMap<String, Any>(), exception.args)
    }
}
