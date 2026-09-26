package com.vertyll.kotlinapi.role.controller

import com.vertyll.kotlinapi.role.dto.RoleCreateDto
import com.vertyll.kotlinapi.role.dto.RoleResponseDto
import com.vertyll.kotlinapi.role.dto.RoleUpdateDto
import com.vertyll.kotlinapi.role.enums.RoleType
import com.vertyll.kotlinapi.role.service.RoleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/roles")
@Tag(name = "Roles", description = "Role management APIs")
class RoleController(
    private val roleService: RoleService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new role")
    fun createRole(
        @RequestBody @Valid dto: RoleCreateDto,
    ): RoleResponseDto = roleService.createRole(dto)

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update existing role")
    fun updateRole(
        @PathVariable id: Long,
        @RequestBody @Valid dto: RoleUpdateDto,
    ): RoleResponseDto = roleService.updateRole(id, dto)

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get role by ID")
    fun getRole(
        @PathVariable id: Long,
    ): RoleResponseDto = roleService.getRoleById(id)

    @GetMapping("/types")
    @Operation(summary = "Get all available role types")
    fun getAllRoleTypes(): List<RoleType> = RoleType.entries
}
