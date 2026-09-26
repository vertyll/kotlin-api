package com.vertyll.kotlinapi.role.dto

import jakarta.validation.constraints.NotBlank

data class RoleUpdateDto(
    @field:NotBlank(message = "validation.role.name.required")
    val name: String = "",
    val description: String? = null,
)
