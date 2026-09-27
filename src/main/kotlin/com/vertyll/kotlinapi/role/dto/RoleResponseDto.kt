package com.vertyll.kotlinapi.role.dto

data class RoleResponseDto(
    val id: Long,
    val name: String,
    val description: String? = null,
)
