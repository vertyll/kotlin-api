package com.vertyll.kotlinapi.user.controller

import com.vertyll.kotlinapi.user.dto.UserCreateDto
import com.vertyll.kotlinapi.user.dto.UserResponseDto
import com.vertyll.kotlinapi.user.dto.UserUpdateDto
import com.vertyll.kotlinapi.user.service.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class UserControllerTest {
    @Mock
    private lateinit var userService: UserService

    @InjectMocks
    private lateinit var userController: UserController

    private val testId = (100..999).random().toLong()

    @Test
    fun `createUser should call service and return created user`() {
        val userCreateDto =
            UserCreateDto(
                firstName = "Test",
                lastName = "User",
                email = "test@example.com",
                password = "password123",
                roleNames = setOf("USER"),
            )
        val userResponseDto =
            UserResponseDto(
                id = testId,
                firstName = "Test",
                lastName = "User",
                email = "test@example.com",
                roles = setOf("USER"),
                enabled = true,
            )
        `when`(userService.createUser(userCreateDto)).thenReturn(userResponseDto)

        val response = userController.createUser(userCreateDto)

        verify(userService).createUser(userCreateDto)
        assertEquals(userResponseDto, response)
    }

    @Test
    fun `updateUser should call service and return updated user`() {
        val id = testId
        val userUpdateDto =
            UserUpdateDto(
                firstName = "Updated",
                lastName = "User",
                email = "updated@example.com",
                roleNames = setOf("USER", "ADMIN"),
            )
        val userResponseDto =
            UserResponseDto(
                id = id,
                firstName = "Updated",
                lastName = "User",
                email = "updated@example.com",
                roles = setOf("USER", "ADMIN"),
                enabled = true,
            )
        `when`(userService.updateUser(id, userUpdateDto)).thenReturn(userResponseDto)

        val response = userController.updateUser(id, userUpdateDto)

        verify(userService).updateUser(id, userUpdateDto)
        assertEquals(userResponseDto, response)
    }

    @Test
    fun `getUser should call service and return user`() {
        val id = testId
        val userResponseDto =
            UserResponseDto(
                id = id,
                firstName = "Test",
                lastName = "User",
                email = "test@example.com",
                roles = setOf("USER"),
                enabled = true,
            )
        `when`(userService.getUserById(id)).thenReturn(userResponseDto)

        val response = userController.getUser(id)

        verify(userService).getUserById(id)
        assertEquals(userResponseDto, response)
    }
}
