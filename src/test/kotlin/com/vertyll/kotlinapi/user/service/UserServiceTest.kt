package com.vertyll.kotlinapi.user.service

import com.vertyll.kotlinapi.common.exception.ApiException
import com.vertyll.kotlinapi.role.model.Role
import com.vertyll.kotlinapi.role.service.RoleService
import com.vertyll.kotlinapi.user.dto.UserCreateDto
import com.vertyll.kotlinapi.user.dto.UserUpdateDto
import com.vertyll.kotlinapi.user.model.User
import com.vertyll.kotlinapi.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class UserServiceTest {
    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var roleService: RoleService

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var userService: UserService

    // Use different IDs for different tests to avoid potential issues with test independence
    private val testUserId = (100..999).random().toLong()
    private val testRoleId = (100..999).random().toLong()
    private val testFirstName = "Test"
    private val testLastName = "User"
    private val testEmail = "test@example.com"
    private val testPassword = "password123"
    private val testEncodedPassword = "encodedPassword123"
    private val testRoleName = "USER"

    @Test
    fun `createUser should create and return a new user when email is unique`() {
        // given
        val createDto =
            UserCreateDto(
                firstName = testFirstName,
                lastName = testLastName,
                email = testEmail,
                password = testPassword,
                roleNames = setOf(testRoleName),
            )

        val testRole = Role(name = testRoleName)
        setRoleId(testRole, testRoleId)

        val savedUser =
            User(
                firstName = testFirstName,
                lastName = testLastName,
                email = testEmail,
                password = testEncodedPassword,
                roles = mutableSetOf(testRole),
                enabled = true,
            )
        setUserId(savedUser, testUserId)

        `when`(userRepository.existsByEmail(testEmail)).thenReturn(false)
        `when`(roleService.getOrCreateDefaultRole(testRoleName)).thenReturn(testRole)
        `when`(passwordEncoder.encode(testPassword)).thenReturn(testEncodedPassword)
        `when`(userRepository.save(any())).thenReturn(savedUser)

        // when
        val result = userService.createUser(createDto)

        // then
        verify(userRepository).existsByEmail(testEmail)
        verify(roleService).getOrCreateDefaultRole(testRoleName)
        verify(passwordEncoder).encode(testPassword)

        val userCaptor = ArgumentCaptor.forClass(User::class.java)
        verify(userRepository).save(userCaptor.capture())

        val capturedUser = userCaptor.value
        assertEquals(testFirstName, capturedUser.firstName)
        assertEquals(testLastName, capturedUser.lastName)
        assertEquals(testEmail, capturedUser.username)
        assertEquals(testEncodedPassword, capturedUser.password)
        assertTrue(capturedUser.roles.any { it.name == testRoleName })
        assertTrue(capturedUser.isEnabled)

        assertEquals(testUserId, result.id)
        assertEquals(testFirstName, result.firstName)
        assertEquals(testLastName, result.lastName)
        assertEquals(testEmail, result.email)
        assertTrue(result.roles.contains(testRoleName))
        assertTrue(result.enabled)
    }

    @Test
    fun `createUser should use default USER role when no roles specified`() {
        // given
        val createDto =
            UserCreateDto(
                firstName = testFirstName,
                lastName = testLastName,
                email = testEmail,
                password = testPassword,
                // No roles specified
            )

        val testRole = Role(name = "USER")
        setRoleId(testRole, testRoleId)

        val savedUser =
            User(
                firstName = testFirstName,
                lastName = testLastName,
                email = testEmail,
                password = testEncodedPassword,
                roles = mutableSetOf(testRole),
                enabled = true,
            )
        setUserId(savedUser, testUserId)

        `when`(userRepository.existsByEmail(testEmail)).thenReturn(false)
        `when`(roleService.getOrCreateDefaultRole("USER")).thenReturn(testRole)
        `when`(passwordEncoder.encode(testPassword)).thenReturn(testEncodedPassword)
        `when`(userRepository.save(any())).thenReturn(savedUser)

        // when
        val result = userService.createUser(createDto)

        // then
        verify(userRepository).existsByEmail(testEmail)
        verify(roleService).getOrCreateDefaultRole("USER")
        verify(passwordEncoder).encode(testPassword)

        val userCaptor = ArgumentCaptor.forClass(User::class.java)
        verify(userRepository).save(userCaptor.capture())

        val capturedUser = userCaptor.value
        assertTrue(capturedUser.roles.any { it.name == "USER" })

        assertTrue(result.roles.contains("USER"))
    }

    @Test
    fun `createUser should throw exception when email already exists`() {
        // given
        val createDto =
            UserCreateDto(
                firstName = testFirstName,
                lastName = testLastName,
                email = testEmail,
                password = testPassword,
            )

        `when`(userRepository.existsByEmail(testEmail)).thenReturn(true)

        // when/then
        val exception =
            assertThrows(ApiException::class.java) {
                userService.createUser(createDto)
            }

        assertEquals("errors.user.emailTaken", exception.message)
        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        verify(userRepository).existsByEmail(testEmail)
        verify(roleService, never()).getOrCreateDefaultRole(anyString())
        verify(passwordEncoder, never()).encode(anyString())
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `updateUser should update and return the user when it exists`() {
        // given
        val existingUser =
            User(
                firstName = testFirstName,
                lastName = testLastName,
                email = testEmail,
                password = testEncodedPassword,
                roles = mutableSetOf(),
                enabled = true,
            )
        setUserId(existingUser, testUserId)

        val updateDto =
            UserUpdateDto(
                firstName = "Updated",
                lastName = "User",
                email = "updated@example.com",
            )

        val updatedUser =
            User(
                firstName = updateDto.firstName,
                lastName = updateDto.lastName,
                email = updateDto.email,
                password = testEncodedPassword,
                roles = mutableSetOf(),
                enabled = true,
            )
        setUserId(updatedUser, testUserId)

        `when`(userRepository.findById(testUserId)).thenReturn(Optional.of(existingUser))
        `when`(userRepository.save(any())).thenReturn(updatedUser)

        // when
        val result = userService.updateUser(testUserId, updateDto)

        // then
        verify(userRepository).findById(testUserId)

        val userCaptor = ArgumentCaptor.forClass(User::class.java)
        verify(userRepository).save(userCaptor.capture())

        val capturedUser = userCaptor.value
        assertEquals(updateDto.firstName, capturedUser.firstName)
        assertEquals(updateDto.lastName, capturedUser.lastName)
        assertEquals(updateDto.email, capturedUser.username)
        assertEquals(testEncodedPassword, capturedUser.password)

        assertEquals(testUserId, result.id)
        assertEquals(updateDto.firstName, result.firstName)
        assertEquals(updateDto.lastName, result.lastName)
        assertEquals(updateDto.email, result.email)
    }

    @Test
    fun `updateUser should throw exception when user does not exist`() {
        // given
        val updateDto =
            UserUpdateDto(
                firstName = "Updated",
                lastName = "User",
                email = "updated@example.com",
            )

        `when`(userRepository.findById(testUserId)).thenReturn(Optional.empty())

        // when/then
        val exception =
            assertThrows(ApiException::class.java) {
                userService.updateUser(testUserId, updateDto)
            }

        assertEquals("errors.user.notFound", exception.message)
        assertEquals(HttpStatus.NOT_FOUND, exception.status)
        verify(userRepository).findById(testUserId)
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `getUserById should return user when it exists`() {
        // given
        val testRole = Role(name = testRoleName)
        setRoleId(testRole, testRoleId)

        val existingUser =
            User(
                firstName = testFirstName,
                lastName = testLastName,
                email = testEmail,
                password = testEncodedPassword,
                roles = mutableSetOf(testRole),
                enabled = true,
            )
        setUserId(existingUser, testUserId)

        `when`(userRepository.findById(testUserId)).thenReturn(Optional.of(existingUser))

        // when
        val result = userService.getUserById(testUserId)

        // then
        verify(userRepository).findById(testUserId)

        assertEquals(testUserId, result.id)
        assertEquals(testFirstName, result.firstName)
        assertEquals(testLastName, result.lastName)
        assertEquals(testEmail, result.email)
        assertTrue(result.roles.contains(testRoleName))
        assertTrue(result.enabled)
    }

    @Test
    fun `getUserById should throw exception when user does not exist`() {
        // given
        `when`(userRepository.findById(testUserId)).thenReturn(Optional.empty())

        // when/then
        val exception =
            assertThrows(ApiException::class.java) {
                userService.getUserById(testUserId)
            }

        assertEquals("errors.user.notFound", exception.message)
        assertEquals(HttpStatus.NOT_FOUND, exception.status)
        verify(userRepository).findById(testUserId)
    }

    // Helper method to set the ID field of a Role using reflection
    private fun setRoleId(
        role: Role,
        id: Long,
    ) {
        val field = role.javaClass.superclass.getDeclaredField("id")
        field.isAccessible = true
        field.set(role, id)
    }

    // Helper method to set the ID field of a User using reflection
    private fun setUserId(
        user: User,
        id: Long,
    ) {
        val field = user.javaClass.superclass.getDeclaredField("id")
        field.isAccessible = true
        field.set(user, id)
    }
}
