package com.vertyll.kotlinapi.role.controller

import com.vertyll.kotlinapi.role.enums.RoleType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/roles")
@Tag(name = "Roles", description = "Realm roles the application understands")
class RoleController {
    @GetMapping("/types")
    @Operation(summary = "Get all role types")
    fun getAllRoleTypes(): List<RoleType> = RoleType.entries
}
