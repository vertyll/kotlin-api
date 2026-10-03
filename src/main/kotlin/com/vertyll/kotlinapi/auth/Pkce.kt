package com.vertyll.kotlinapi.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object Pkce {
    const val CHALLENGE_METHOD = "S256"

    private const val STATE_BYTES = 32
    private const val VERIFIER_BYTES = 64
    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun newState(): String = randomString(STATE_BYTES)

    fun newCodeVerifier(): String = randomString(VERIFIER_BYTES)

    fun challengeOf(codeVerifier: String): String =
        encoder.encodeToString(MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(Charsets.US_ASCII)))

    fun sameState(
        expected: String,
        received: String,
    ): Boolean = MessageDigest.isEqual(expected.toByteArray(Charsets.US_ASCII), received.toByteArray(Charsets.US_ASCII))

    private fun randomString(bytes: Int): String = encoder.encodeToString(ByteArray(bytes).also(random::nextBytes))
}
