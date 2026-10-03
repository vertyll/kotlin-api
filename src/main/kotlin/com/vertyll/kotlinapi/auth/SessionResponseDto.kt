package com.vertyll.kotlinapi.auth

data class SessionResponseDto(
    val userId: String,
    val email: String,
    val roles: Set<String>,
) {
    companion object {
        fun from(session: AuthSession): SessionResponseDto =
            SessionResponseDto(session.identity.keycloakId, session.identity.email, session.identity.roles)
    }
}
