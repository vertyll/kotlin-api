package com.vertyll.kotlinapi.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class ChangeEmailRequestDto(
    @field:NotBlank(message = "validation.currentPassword.required")
    val currentPassword: String = "",
    @field:NotBlank(message = "validation.email.required")
    @field:Email(message = "validation.email.invalid")
    val newEmail: String = "",
)
