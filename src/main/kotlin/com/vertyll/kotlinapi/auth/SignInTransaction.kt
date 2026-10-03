package com.vertyll.kotlinapi.auth

import java.io.Serializable

class SignInTransaction(
    val state: String,
    val codeVerifier: String,
) : Serializable {
    override fun toString(): String = "SignInTransaction(state=***, codeVerifier=***)"

    private companion object {
        private const val serialVersionUID = 1L
    }
}
