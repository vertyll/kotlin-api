package com.vertyll.kotlinapi.auth

data class SessionResponseDto(
    val userId: String,
    val email: String,
    val roles: Set<String>,
) {
    companion object {
        fun from(identity: KeycloakIdentity): SessionResponseDto =
            SessionResponseDto(userId = identity.keycloakId, email = identity.email, roles = identity.roles)
    }
}
