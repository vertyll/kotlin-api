package com.vertyll.kotlinapi.auth

import com.vertyll.kotlinapi.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SessionService(
    private val tokens: KeycloakTokenClient,
    private val users: UserService,
) {
    private val log = LoggerFactory.getLogger(SessionService::class.java)

    fun signIn(
        code: String,
        codeVerifier: String,
    ): AuthSession {
        val session = tokens.exchange(code, codeVerifier)
        var provisioned = false
        try {
            users.sync(session.identity)
            provisioned = true
        } finally {
            if (!provisioned) {
                tokens.revoke(session.refreshToken)
            }
        }
        log.info("User {} signed in", session.identity.keycloakId)
        return session
    }

    fun refresh(session: AuthSession): AuthSession = tokens.refresh(session.refreshToken)

    fun signOut(session: AuthSession) {
        tokens.revoke(session.refreshToken)
        log.info("User {} signed out", session.identity.keycloakId)
    }
}
