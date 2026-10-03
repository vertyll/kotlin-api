package com.vertyll.kotlinapi.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class BrowserSessions {
    fun begin(request: HttpServletRequest): SignInTransaction =
        SignInTransaction(Pkce.newState(), Pkce.newCodeVerifier()).also {
            request.getSession(true).setAttribute(TRANSACTION, it)
        }

    fun takeTransaction(request: HttpServletRequest): SignInTransaction? {
        val session = request.getSession(false) ?: return null
        val transaction = session.getAttribute(TRANSACTION) as? SignInTransaction
        session.removeAttribute(TRANSACTION)
        return transaction
    }

    fun establish(
        request: HttpServletRequest,
        authSession: AuthSession,
    ) {
        end(request)
        request.getSession(true).setAttribute(SESSION, authSession)
    }

    fun current(request: HttpServletRequest): AuthSession? = request.getSession(false)?.getAttribute(SESSION) as? AuthSession

    fun replace(
        request: HttpServletRequest,
        authSession: AuthSession,
    ) {
        request.getSession(false)?.setAttribute(SESSION, authSession)
    }

    fun end(request: HttpServletRequest) {
        request.getSession(false)?.invalidate()
    }

    private companion object {
        private val TRANSACTION = "${BrowserSessions::class.java.name}.transaction"
        private val SESSION = "${BrowserSessions::class.java.name}.session"
    }
}
