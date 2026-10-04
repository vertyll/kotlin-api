package com.vertyll.kotlinapi.auth

data class SessionResponseDto(
    val userId: String,
    val email: String,
    val roles: Set<String>,
) {
    companion object {
        fun from(identity: KeycloakIdentity): SessionResponseDto = SessionResponseDto(identity.keycloakId, identity.email, identity.roles)
    }
}
