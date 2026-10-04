package com.vertyll.kotlinapi.auth

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.testcontainers.containers.GenericContainer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SharedRefreshesTest {
    @Test
    fun `two replicas refreshing one token reach Keycloak once`() {
        val first = SharedRefreshes(redis, RedisKeyProperties("test-a"))
        val second = SharedRefreshes(redis, RedisKeyProperties("test-a"))
        val calls = AtomicInteger()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)

        val leader =
            CompletableFuture.supplyAsync {
                first.refresh("refresh-1") {
                    calls.incrementAndGet()
                    entered.countDown()
                    release.await(5, TimeUnit.SECONDS)
                    SharedRefreshes.TokenPair("access-2", "refresh-2")
                }
            }
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue()
        val follower =
            CompletableFuture.supplyAsync {
                second.refresh("refresh-1") {
                    calls.incrementAndGet()
                    SharedRefreshes.TokenPair("access-3", "refresh-3")
                }
            }
        release.countDown()

        assertThat(leader.get(10, TimeUnit.SECONDS).refreshToken).isEqualTo("refresh-2")
        assertThat(follower.get(10, TimeUnit.SECONDS).refreshToken).isEqualTo("refresh-2")
        assertThat(calls).hasValue(1)
    }

    @Test
    fun `a stale request receives the tokens already issued`() {
        SharedRefreshes(redis, RedisKeyProperties("test-b")).refresh("refresh-1") { SharedRefreshes.TokenPair("access-2", "refresh-2") }

        val stale = SharedRefreshes(redis, RedisKeyProperties("test-b")).refresh("refresh-1") { error("Keycloak must not be asked twice") }

        assertThat(stale.accessToken).isEqualTo("access-2")
    }

    @Test
    fun `a refused refresh is not shared`() {
        val replica = SharedRefreshes(redis, RedisKeyProperties("test-c"))
        assertThatThrownBy { replica.refresh("refresh-1") { error("refused") } }.isInstanceOf(IllegalStateException::class.java)

        val retried = replica.refresh("refresh-1") { SharedRefreshes.TokenPair("access-2", "refresh-2") }

        assertThat(retried.refreshToken).isEqualTo("refresh-2")
    }

    @Test
    fun `keys carry the application prefix and never the token`() {
        SharedRefreshes(redis, RedisKeyProperties("test-d")).refresh("secret-refresh") { SharedRefreshes.TokenPair("access", "refresh") }

        assertThat(redis.keys("test-d:refresh-result:*")).hasSize(1)
        assertThat(redis.keys("*secret-refresh*")).isEmpty()
    }

    companion object {
        private val container = GenericContainer("redis:8-alpine").withExposedPorts(6379)
        private lateinit var connections: LettuceConnectionFactory
        private lateinit var redis: StringRedisTemplate

        @JvmStatic
        @BeforeAll
        fun startRedis() {
            container.start()
            connections = LettuceConnectionFactory(container.host, container.getMappedPort(6379))
            connections.afterPropertiesSet()
            connections.start()
            redis = StringRedisTemplate(connections)
        }

        @JvmStatic
        @AfterAll
        fun stopRedis() {
            connections.destroy()
            container.stop()
        }
    }
}
