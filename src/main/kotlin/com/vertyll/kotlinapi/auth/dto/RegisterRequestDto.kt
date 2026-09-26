package com.vertyll.kotlinapi.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class RegisterRequestDto(
    @field:NotBlank(message = "validation.firstName.required")
    val firstName: String = "",
    @field:NotBlank(message = "validation.lastName.required")
    val lastName: String = "",
    @field:NotBlank(message = "validation.email.required")
    @field:Email(message = "validation.email.invalid")
    val email: String = "",
    @field:NotBlank(message = "validation.password.required")
    val password: String = "",
)
