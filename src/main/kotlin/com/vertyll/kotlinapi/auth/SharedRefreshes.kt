package com.vertyll.kotlinapi.auth

import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.util.HexFormat

@Component
class SharedRefreshes(
    private val redis: StringRedisTemplate?,
    private val properties: RedisKeyProperties,
) {
    private val keyPrefix = properties.keyPrefix

    private val log = LoggerFactory.getLogger(SharedRefreshes::class.java)

    fun refresh(
        refreshToken: String,
        keycloak: () -> TokenPair,
    ): TokenPair {
        val store = redis ?: return keycloak()
        val id = sha256(refreshToken)
        val lockKey = "$keyPrefix:refresh-lock:$id"
        val resultKey = "$keyPrefix:refresh-result:$id"
        val shared: TokenPair?
        val leader: Boolean
        try {
            shared = read(store, resultKey)
            leader = shared == null && store.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL) == true
        } catch (e: DataAccessException) {
            log.warn("Redis unavailable, refreshing without coordinating replicas: {}", e.message)
            return keycloak()
        }
        return when {
            shared != null -> shared
            leader -> lead(store, lockKey, resultKey, keycloak)
            else -> awaitOtherReplica(store, lockKey, resultKey) ?: keycloak()
        }
    }

    private fun lead(
        store: StringRedisTemplate,
        lockKey: String,
        resultKey: String,
        keycloak: () -> TokenPair,
    ): TokenPair {
        var refreshed = false
        val pair: TokenPair
        try {
            pair = keycloak()
            refreshed = true
        } finally {
            if (!refreshed) {
                release(store, lockKey)
            }
        }
        try {
            store.opsForValue().set(resultKey, "${pair.accessToken}$SEPARATOR${pair.refreshToken}", RESULT_TTL)
        } catch (e: DataAccessException) {
            log.warn("Could not share the refreshed tokens with other replicas: {}", e.message)
        }
        return pair
    }

    private fun awaitOtherReplica(
        store: StringRedisTemplate,
        lockKey: String,
        resultKey: String,
    ): TokenPair? {
        try {
            repeat(WAIT_ATTEMPTS) {
                Thread.sleep(WAIT_INTERVAL)
                val shared = read(store, resultKey)
                if (shared != null || store.hasKey(lockKey) != true) {
                    return shared
                }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.debug("Interrupted while waiting for another replica's refresh", e)
        } catch (e: DataAccessException) {
            log.warn("Redis unavailable while waiting for another replica's refresh: {}", e.message)
        }
        return null
    }

    private fun read(
        store: StringRedisTemplate,
        resultKey: String,
    ): TokenPair? {
        val value = store.opsForValue().get(resultKey) ?: return null
        val separator = value.indexOf(SEPARATOR)
        return if (separator < 0) null else TokenPair(value.substring(0, separator), value.substring(separator + 1))
    }

    private fun release(
        store: StringRedisTemplate,
        lockKey: String,
    ) {
        try {
            store.delete(lockKey)
        } catch (e: DataAccessException) {
            log.warn("Could not release the refresh lock, it expires on its own: {}", e.message)
        }
    }

    data class TokenPair(
        val accessToken: String,
        val refreshToken: String,
    ) {
        override fun toString(): String = "TokenPair(accessToken=***, refreshToken=***)"
    }

    companion object {
        private val LOCK_TTL = Duration.ofSeconds(10)
        private val RESULT_TTL = Duration.ofSeconds(30)
        private val WAIT_INTERVAL = Duration.ofMillis(100)
        private const val WAIT_ATTEMPTS = 50
        private const val SEPARATOR = "\n"

        fun inProcessOnly(): SharedRefreshes = SharedRefreshes(null, RedisKeyProperties(""))

        private fun sha256(value: String): String =
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8)))
    }
}
