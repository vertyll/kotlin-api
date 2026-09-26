package com.vertyll.kotlinapi.auth.dto

import java.time.LocalDateTime

data class SessionResponseDto(
    val id: Long,
    val deviceInfo: String?,
    val createdAt: LocalDateTime,
)
