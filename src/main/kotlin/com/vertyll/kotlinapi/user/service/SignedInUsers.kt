package com.vertyll.kotlinapi.user.service

import com.vertyll.kotlinapi.auth.KeycloakIdentity
import com.vertyll.kotlinapi.auth.SignedInAccounts
import org.springframework.stereotype.Component

@Component
class SignedInUsers(
    private val users: UserService,
) : SignedInAccounts {
    override fun signedIn(identity: KeycloakIdentity) {
        users.sync(identity)
    }
}
