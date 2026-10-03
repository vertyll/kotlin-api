package com.vertyll.kotlinapi.user.dto

data class UserResponseDto(
    val id: Long,
    val keycloakId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val roles: Set<String>,
)
