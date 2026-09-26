package com.vertyll.kotlinapi.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UserUpdateDto(
    @field:NotBlank(message = "validation.firstName.required")
    val firstName: String = "",
    @field:NotBlank(message = "validation.lastName.required")
    val lastName: String = "",
    @field:NotBlank(message = "validation.email.required")
    @field:Email(message = "validation.email.invalid")
    val email: String = "",
    val password: String? = null,
    val roleNames: Set<String> = emptySet(),
)
