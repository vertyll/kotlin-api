package com.vertyll.kotlinapi.auth.dto

data class AuthResponseDto(
    val token: String,
    val type: String,
)
