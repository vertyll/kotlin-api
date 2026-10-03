package com.vertyll.kotlinapi

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestContainer::class, RedisTestContainer::class)
class KotlinApiApplicationTests {
    @Test
    fun contextLoads() = Unit
}
