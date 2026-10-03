package com.vertyll.kotlinapi.auth

import java.io.Serializable
import java.time.Duration
import java.time.Instant

class AuthSession(
    val identity: KeycloakIdentity,
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: Instant,
) : Serializable {
    fun needsRefreshAt(
        now: Instant,
        skew: Duration,
    ): Boolean = !now.plus(skew).isBefore(accessTokenExpiresAt)

    override fun toString(): String =
        "AuthSession(identity=$identity, accessToken=***, refreshToken=***, accessTokenExpiresAt=$accessTokenExpiresAt)"

    private companion object {
        private const val serialVersionUID = 1L
    }
}
