package com.vertyll.kotlinapi.auth.dto

import jakarta.validation.constraints.NotBlank

data class ChangePasswordRequestDto(
    @field:NotBlank(message = "validation.currentPassword.required")
    val currentPassword: String = "",
    @field:NotBlank(message = "validation.newPassword.required")
    val newPassword: String = "",
)
