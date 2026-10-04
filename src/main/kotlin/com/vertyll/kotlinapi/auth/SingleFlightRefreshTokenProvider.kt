package com.vertyll.kotlinapi.auth

import org.springframework.security.oauth2.client.OAuth2AuthorizationContext
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class SingleFlightRefreshTokenProvider(
    private val refresh: OAuth2AuthorizedClientProvider,
    private val sharedRefreshes: SharedRefreshes,
    private val clock: Clock,
) : OAuth2AuthorizedClientProvider {
    private val refreshes = ConcurrentHashMap<String, Refresh>()

    override fun authorize(context: OAuth2AuthorizationContext): OAuth2AuthorizedClient? {
        val current = context.authorizedClient
        val refreshToken = current?.refreshToken
        if (current == null || refreshToken == null || !expiresSoon(current.accessToken)) {
            return refresh.authorize(context)
        }
        forgetOldRefreshes()

        val mine = Refresh()
        val running = refreshes.putIfAbsent(refreshToken.tokenValue, mine)
        if (running != null) {
            val outcome = running.await()
            return if (outcome.completed) outcome.client else refreshShared(context, current, refreshToken)
        }

        var finished = false
        try {
            val refreshed = refreshShared(context, current, refreshToken)
            mine.finish(refreshed, clock.instant())
            finished = true
            return refreshed
        } finally {
            if (!finished) {
                refreshes.remove(refreshToken.tokenValue, mine)
                mine.abandon()
            }
        }
    }

    private fun refreshShared(
        context: OAuth2AuthorizationContext,
        current: OAuth2AuthorizedClient,
        refreshToken: OAuth2RefreshToken,
    ): OAuth2AuthorizedClient {
        val tokens =
            sharedRefreshes.refresh(refreshToken.tokenValue) {
                val refreshed =
                    checkNotNull(refresh.authorize(context)) { "The refresh provider declined an expired access token" }
                val access = refreshed.accessToken
                val issuedAt = access.issuedAt ?: clock.instant()
                SharedRefreshes.TokenPair(
                    access.tokenValue,
                    refreshed.refreshToken?.tokenValue ?: refreshToken.tokenValue,
                    issuedAt,
                    access.expiresAt ?: issuedAt,
                )
            }
        return OAuth2AuthorizedClient(
            current.clientRegistration,
            current.principalName,
            OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokens.accessToken,
                tokens.issuedAt,
                tokens.expiresAt,
                current.accessToken.scopes,
            ),
            OAuth2RefreshToken(tokens.refreshToken, tokens.issuedAt),
        )
    }

    private fun expiresSoon(accessToken: OAuth2AccessToken): Boolean {
        val expiresAt = accessToken.expiresAt ?: return false
        return !clock.instant().isBefore(expiresAt.minus(CLOCK_SKEW))
    }

    private fun forgetOldRefreshes() {
        val oldest = clock.instant().minus(REUSE_WINDOW)
        refreshes.values.removeIf { it.finishedBefore(oldest) }
    }

    private data class Outcome(
        val client: OAuth2AuthorizedClient?,
        val completed: Boolean,
        val at: Instant,
    )

    private class Refresh {
        private val result = CompletableFuture<Outcome>()

        fun finish(
            client: OAuth2AuthorizedClient?,
            at: Instant,
        ) {
            result.complete(Outcome(client, true, at))
        }

        fun abandon() {
            result.complete(Outcome(null, false, Instant.MIN))
        }

        fun await(): Outcome = result.join()

        fun finishedBefore(instant: Instant): Boolean = result.getNow(null)?.at?.isBefore(instant) == true
    }

    private companion object {
        private val REUSE_WINDOW = Duration.ofSeconds(30)
        private val CLOCK_SKEW = Duration.ofSeconds(60)
    }
}
