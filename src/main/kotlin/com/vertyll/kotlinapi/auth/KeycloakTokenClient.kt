package com.vertyll.kotlinapi.auth

import com.fasterxml.jackson.annotation.JsonProperty
import com.vertyll.kotlinapi.common.exception.ApiException
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@Component
class KeycloakTokenClient(
    private val keycloak: KeycloakProperties,
    private val auth: AuthProperties,
    private val jwtDecoder: JwtDecoder,
    private val clock: Clock,
    private val restClient: RestClient,
) {
    private val log = LoggerFactory.getLogger(KeycloakTokenClient::class.java)
    private val refreshes = ConcurrentHashMap<String, Refresh>()

    fun exchange(
        code: String,
        codeVerifier: String,
    ): AuthSession =
        post(
            form(
                GRANT_TYPE to "authorization_code",
                "code" to code,
                "code_verifier" to codeVerifier,
                "redirect_uri" to auth.callbackUrl,
            ),
            AuthErrors.SIGN_IN_REJECTED,
        )

    fun refresh(refreshToken: String): AuthSession {
        forgetOldRefreshes()
        val mine = Refresh()
        val running = refreshes.putIfAbsent(refreshToken, mine)
        return if (running != null) shared(running.await(), refreshToken) else refreshAs(mine, refreshToken)
    }

    fun revoke(refreshToken: String) {
        try {
            restClient
                .post()
                .uri(keycloak.endpoint("logout"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form(REFRESH_TOKEN to refreshToken))
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientException) {
            log.warn("Keycloak did not end the session: {}", e.message)
        }
    }

    private fun shared(
        outcome: Outcome,
        refreshToken: String,
    ): AuthSession {
        val failure = outcome.failure
        return when {
            outcome.session != null -> outcome.session
            failure != null -> throw ApiException(failure.messageKey, failure.status, cause = failure)
            else -> requestRefresh(refreshToken)
        }
    }

    private fun refreshAs(
        mine: Refresh,
        refreshToken: String,
    ): AuthSession {
        try {
            val session = requestRefresh(refreshToken)
            mine.finish(Outcome(session, null, clock.instant()))
            return session
        } catch (e: ApiException) {
            refreshes.remove(refreshToken, mine)
            mine.finish(Outcome(null, e, clock.instant()))
            throw e
        } finally {
            if (!mine.isFinished()) {
                refreshes.remove(refreshToken, mine)
                mine.finish(Outcome(null, null, clock.instant()))
            }
        }
    }

    private fun requestRefresh(refreshToken: String): AuthSession =
        post(form(GRANT_TYPE to REFRESH_TOKEN, REFRESH_TOKEN to refreshToken), AuthErrors.SESSION_EXPIRED)

    private fun forgetOldRefreshes() {
        val oldest = clock.instant().minus(REUSE_WINDOW)
        refreshes.values.removeIf { it.finishedBefore(oldest) }
    }

    private fun form(vararg fields: Pair<String, String>): MultiValueMap<String, String> =
        LinkedMultiValueMap<String, String>().apply {
            add("client_id", keycloak.clientId)
            add("client_secret", keycloak.clientSecret)
            fields.forEach { (name, value) -> add(name, value) }
        }

    private fun post(
        form: MultiValueMap<String, String>,
        onRejection: String,
    ): AuthSession {
        val response =
            try {
                restClient
                    .post()
                    .uri(keycloak.endpoint("token"))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse::class.java)
            } catch (e: RestClientResponseException) {
                throw if (e.statusCode.is4xxClientError) AuthErrors.rejected(onRejection, e) else AuthErrors.unavailable(e)
            } catch (e: RestClientException) {
                throw AuthErrors.unavailable(e)
            }
        val accessToken = response?.accessToken ?: throw AuthErrors.unavailable()
        val refreshToken = response.refreshToken ?: throw AuthErrors.unavailable()
        return toSession(accessToken, refreshToken, onRejection)
    }

    private fun toSession(
        accessToken: String,
        refreshToken: String,
        onRejection: String,
    ): AuthSession {
        val jwt =
            try {
                jwtDecoder.decode(accessToken)
            } catch (e: JwtException) {
                throw AuthErrors.rejected(onRejection, e)
            }
        val expiresAt = jwt.expiresAt ?: throw AuthErrors.rejected(onRejection)
        val identity =
            runCatching { KeycloakIdentity.from(jwt) }.getOrElse { throw AuthErrors.rejected(onRejection, it) }
        return AuthSession(identity, accessToken, refreshToken, expiresAt)
    }

    private data class TokenResponse(
        @param:JsonProperty("access_token") val accessToken: String?,
        @param:JsonProperty("refresh_token") val refreshToken: String?,
    )

    private data class Outcome(
        val session: AuthSession?,
        val failure: ApiException?,
        val at: Instant,
    )

    private class Refresh {
        private val result = CompletableFuture<Outcome>()

        fun finish(outcome: Outcome) {
            result.complete(outcome)
        }

        fun isFinished(): Boolean = result.isDone

        fun await(): Outcome = result.join()

        fun finishedBefore(instant: Instant): Boolean = result.getNow(null)?.at?.isBefore(instant) ?: false
    }

    private companion object {
        private const val GRANT_TYPE = "grant_type"
        private const val REFRESH_TOKEN = "refresh_token"
        private val REUSE_WINDOW = Duration.ofSeconds(30)
    }
}
