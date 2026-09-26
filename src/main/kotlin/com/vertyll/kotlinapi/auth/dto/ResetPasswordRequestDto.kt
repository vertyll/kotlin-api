package com.vertyll.kotlinapi.auth.dto

import jakarta.validation.constraints.NotBlank

data class ResetPasswordRequestDto(
    @field:NotBlank(message = "validation.newPassword.required")
    val newPassword: String = "",
)
