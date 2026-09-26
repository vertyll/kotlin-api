package com.vertyll.kotlinapi.role.service

import com.vertyll.kotlinapi.common.exception.ApiException
import com.vertyll.kotlinapi.role.dto.RoleCreateDto
import com.vertyll.kotlinapi.role.dto.RoleUpdateDto
import com.vertyll.kotlinapi.role.model.Role
import com.vertyll.kotlinapi.role.repository.RoleRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class RoleServiceTest {
    @Mock
    private lateinit var roleRepository: RoleRepository

    @InjectMocks
    private lateinit var roleService: RoleService

    // Use different IDs for different tests to avoid potential issues with test independence
    private val testRoleId = (100..999).random().toLong()
    private val testRoleName = "TEST_ROLE"
    private val testRoleDescription = "Test role description"

    @Test
    fun `createRole should create and return a new role when name is unique`() {
        // given
        val createDto =
            RoleCreateDto(
                name = testRoleName,
                description = testRoleDescription,
            )

        val savedRole =
            Role(
                name = testRoleName,
                description = testRoleDescription,
            )
        // Set ID field using reflection since it's in the BaseEntity class
        setRoleId(savedRole, testRoleId)

        `when`(roleRepository.existsByName(testRoleName)).thenReturn(false)
        `when`(roleRepository.save(any())).thenReturn(savedRole)

        // when
        val result = roleService.createRole(createDto)

        // then
        verify(roleRepository).existsByName(testRoleName)

        val roleCaptor = ArgumentCaptor.forClass(Role::class.java)
        verify(roleRepository).save(roleCaptor.capture())

        val capturedRole = roleCaptor.value
        assertEquals(testRoleName, capturedRole.name)
        assertEquals(testRoleDescription, capturedRole.description)

        assertEquals(testRoleId, result.id)
        assertEquals(testRoleName, result.name)
        assertEquals(testRoleDescription, result.description)
    }

    @Test
    fun `createRole should throw exception when role with same name already exists`() {
        // given
        val createDto =
            RoleCreateDto(
                name = testRoleName,
                description = testRoleDescription,
            )

        `when`(roleRepository.existsByName(testRoleName)).thenReturn(true)

        // when/then
        val exception =
            assertThrows(ApiException::class.java) {
                roleService.createRole(createDto)
            }

        assertEquals("errors.role.alreadyExists", exception.message)
        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        verify(roleRepository).existsByName(testRoleName)
        verify(roleRepository, never()).save(any())
    }

    @Test
    fun `updateRole should update and return the role when it exists and name is unique`() {
        // given
        val existingRole =
            Role(
                name = testRoleName,
                description = testRoleDescription,
            )
        setRoleId(existingRole, testRoleId)

        val updateDto =
            RoleUpdateDto(
                name = "UPDATED_ROLE",
                description = "Updated description",
            )

        val updatedRole =
            existingRole.copy(
                name = updateDto.name,
                description = updateDto.description,
            )
        setRoleId(updatedRole, testRoleId)

        `when`(roleRepository.findById(testRoleId)).thenReturn(Optional.of(existingRole))
        `when`(roleRepository.existsByName(updateDto.name)).thenReturn(false)
        `when`(roleRepository.save(any())).thenReturn(updatedRole)

        // when
        val result = roleService.updateRole(testRoleId, updateDto)

        // then
        verify(roleRepository).findById(testRoleId)
        verify(roleRepository).existsByName(updateDto.name)

        val roleCaptor = ArgumentCaptor.forClass(Role::class.java)
        verify(roleRepository).save(roleCaptor.capture())

        val capturedRole = roleCaptor.value
        assertEquals(updateDto.name, capturedRole.name)
        assertEquals(updateDto.description, capturedRole.description)

        assertEquals(testRoleId, result.id)
        assertEquals(updateDto.name, result.name)
        assertEquals(updateDto.description, result.description)
    }

    @Test
    fun `updateRole should throw exception when role does not exist`() {
        // given
        val updateDto =
            RoleUpdateDto(
                name = "UPDATED_ROLE",
                description = "Updated description",
            )

        `when`(roleRepository.findById(testRoleId)).thenReturn(Optional.empty())

        // when/then
        val exception =
            assertThrows(ApiException::class.java) {
                roleService.updateRole(testRoleId, updateDto)
            }

        assertEquals("errors.role.notFound", exception.message)
        assertEquals(HttpStatus.NOT_FOUND, exception.status)
        verify(roleRepository).findById(testRoleId)
        verify(roleRepository, never()).existsByName(anyString())
        verify(roleRepository, never()).save(any())
    }

    @Test
    fun `updateRole should throw exception when new name already exists for another role`() {
        // given
        val existingRole =
            Role(
                name = testRoleName,
                description = testRoleDescription,
            )
        setRoleId(existingRole, testRoleId)

        val updateDto =
            RoleUpdateDto(
                name = "EXISTING_ROLE",
                description = "Updated description",
            )

        `when`(roleRepository.findById(testRoleId)).thenReturn(Optional.of(existingRole))
        `when`(roleRepository.existsByName(updateDto.name)).thenReturn(true)

        // when/then
        val exception =
            assertThrows(ApiException::class.java) {
                roleService.updateRole(testRoleId, updateDto)
            }

        assertEquals("errors.role.alreadyExists", exception.message)
        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        verify(roleRepository).findById(testRoleId)
        verify(roleRepository).existsByName(updateDto.name)
        verify(roleRepository, never()).save(any())
    }

    @Test
    fun `updateRole should allow updating to the same name`() {
        // given
        val existingRole =
            Role(
                name = testRoleName,
                description = testRoleDescription,
            )
        setRoleId(existingRole, testRoleId)

        val updateDto =
            RoleUpdateDto(
                name = testRoleName,
                description = "Updated description",
            )

        val updatedRole =
            existingRole.copy(
                name = updateDto.name,
                description = updateDto.description,
            )
        setRoleId(updatedRole, testRoleId)

        `when`(roleRepository.findById(testRoleId)).thenReturn(Optional.of(existingRole))
        `when`(roleRepository.existsByName(updateDto.name)).thenReturn(true) // Name exists but it's the same role
        `when`(roleRepository.save(any())).thenReturn(updatedRole)

        // when
        val result = roleService.updateRole(testRoleId, updateDto)

        // then
        verify(roleRepository).findById(testRoleId)
        verify(roleRepository).existsByName(updateDto.name)

        val roleCaptor = ArgumentCaptor.forClass(Role::class.java)
        verify(roleRepository).save(roleCaptor.capture())

        assertEquals(testRoleId, result.id)
        assertEquals(updateDto.name, result.name)
        assertEquals(updateDto.description, result.description)
    }

    @Test
    fun `getOrCreateDefaultRole should return existing role when it exists`() {
        // given
        val existingRole =
            Role(
                name = testRoleName,
                description = testRoleDescription,
            )
        setRoleId(existingRole, testRoleId)

        `when`(roleRepository.findByName(testRoleName)).thenReturn(Optional.of(existingRole))

        // when
        val result = roleService.getOrCreateDefaultRole(testRoleName)

        // then
        verify(roleRepository).findByName(testRoleName)
        verify(roleRepository, never()).save(any())

        assertEquals(testRoleName, result.name)
        assertEquals(testRoleDescription, result.description)
    }

    @Test
    fun `getOrCreateDefaultRole should create and return new role when it does not exist`() {
        // given
        val newRole =
            Role(
                name = testRoleName,
                description = "Default role: $testRoleName",
            )
        setRoleId(newRole, testRoleId)

        `when`(roleRepository.findByName(testRoleName)).thenReturn(Optional.empty())
        `when`(roleRepository.save(any())).thenReturn(newRole)

        // when
        val result = roleService.getOrCreateDefaultRole(testRoleName)

        // then
        verify(roleRepository).findByName(testRoleName)

        val roleCaptor = ArgumentCaptor.forClass(Role::class.java)
        verify(roleRepository).save(roleCaptor.capture())

        val capturedRole = roleCaptor.value
        assertEquals(testRoleName, capturedRole.name)
        assertEquals("Default role: $testRoleName", capturedRole.description)

        assertEquals(testRoleName, result.name)
        assertEquals("Default role: $testRoleName", result.description)
    }

    @Test
    fun `getRoleById should return role when it exists`() {
        // given
        val existingRole =
            Role(
                name = testRoleName,
                description = testRoleDescription,
            )
        setRoleId(existingRole, testRoleId)

        `when`(roleRepository.findById(testRoleId)).thenReturn(Optional.of(existingRole))

        // when
        val result = roleService.getRoleById(testRoleId)

        // then
        verify(roleRepository).findById(testRoleId)

        assertEquals(testRoleId, result.id)
        assertEquals(testRoleName, result.name)
        assertEquals(testRoleDescription, result.description)
    }

    @Test
    fun `getRoleById should throw exception when role does not exist`() {
        // given
        `when`(roleRepository.findById(testRoleId)).thenReturn(Optional.empty())

        // when/then
        val exception =
            assertThrows(ApiException::class.java) {
                roleService.getRoleById(testRoleId)
            }

        assertEquals("errors.role.notFound", exception.message)
        assertEquals(HttpStatus.NOT_FOUND, exception.status)
        verify(roleRepository).findById(testRoleId)
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
}
