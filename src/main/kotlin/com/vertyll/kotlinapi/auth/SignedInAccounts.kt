package com.vertyll.kotlinapi.auth

/**
 * The application's own record of the people who sign in, which a sign-in brings in step with the verified token
 * before it completes. The sign-in owns this contract; the module that keeps the accounts implements it, so the
 * dependency runs from accounts to the sign-in and never back.
 */
fun interface SignedInAccounts {
    /** Creates or updates the account of the person [identity] describes; throws when it cannot be stored. */
    fun signedIn(identity: KeycloakIdentity)
}
