package com.vertyll.kotlinapi.user.dto

data class UserResponseDto(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val roles: Set<String>,
    val enabled: Boolean,
)
