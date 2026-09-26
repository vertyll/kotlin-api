package com.vertyll.kotlinapi.role.controller

import com.vertyll.kotlinapi.role.dto.RoleCreateDto
import com.vertyll.kotlinapi.role.dto.RoleResponseDto
import com.vertyll.kotlinapi.role.dto.RoleUpdateDto
import com.vertyll.kotlinapi.role.enums.RoleType
import com.vertyll.kotlinapi.role.service.RoleService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class RoleControllerTest {
    @Mock
    private lateinit var roleService: RoleService

    @InjectMocks
    private lateinit var roleController: RoleController

    // Use different IDs for different tests to avoid potential issues with test independence
    private val testId = (100..999).random().toLong()

    @Test
    fun `createRole should call service and return created role`() {
        // given
        val roleCreateDto =
            RoleCreateDto(
                name = "TEST_ROLE",
                description = "Test role description",
            )
        val roleResponseDto =
            RoleResponseDto(
                id = testId,
                name = "TEST_ROLE",
                description = "Test role description",
            )
        `when`(roleService.createRole(roleCreateDto)).thenReturn(roleResponseDto)

        // when
        val response = roleController.createRole(roleCreateDto)

        // then
        verify(roleService).createRole(roleCreateDto)
        assertEquals(roleResponseDto, response)
    }

    @Test
    fun `updateRole should call service and return updated role`() {
        // given
        val id = testId
        val roleUpdateDto =
            RoleUpdateDto(
                name = "UPDATED_ROLE",
                description = "Updated role description",
            )
        val roleResponseDto =
            RoleResponseDto(
                id = id,
                name = "UPDATED_ROLE",
                description = "Updated role description",
            )
        `when`(roleService.updateRole(id, roleUpdateDto)).thenReturn(roleResponseDto)

        // when
        val response = roleController.updateRole(id, roleUpdateDto)

        // then
        verify(roleService).updateRole(id, roleUpdateDto)
        assertEquals(roleResponseDto, response)
    }

    @Test
    fun `getRole should call service and return role`() {
        // given
        val id = testId
        val roleResponseDto =
            RoleResponseDto(
                id = id,
                name = "TEST_ROLE",
                description = "Test role description",
            )
        `when`(roleService.getRoleById(id)).thenReturn(roleResponseDto)

        // when
        val response = roleController.getRole(id)

        // then
        verify(roleService).getRoleById(id)
        assertEquals(roleResponseDto, response)
    }

    @Test
    fun `getAllRoleTypes should return all role types`() {
        // when
        val response = roleController.getAllRoleTypes()

        // then
        assertEquals(RoleType.entries, response)

        // Verify all expected role types are present
        val roleTypes = response
        assertNotNull(roleTypes)
        assertTrue(roleTypes!!.contains(RoleType.ADMIN))
        assertTrue(roleTypes.contains(RoleType.USER))
        assertTrue(roleTypes.contains(RoleType.MANAGER))
        assertTrue(roleTypes.contains(RoleType.EMPLOYEE))
        assertEquals(4, roleTypes.size)
    }
}
