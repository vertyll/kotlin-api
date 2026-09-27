package com.vertyll.kotlinapi.common.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BaseEntityTest {
    private class TestEntity : BaseEntity()

    @Test
    fun auditFields_ShouldBeSettableAndGettable() {
        val entity = TestEntity()
        val now = LocalDateTime.now()
        val user = "testUser"

        entity.createdAt = now
        entity.updatedAt = now
        entity.createdBy = user
        entity.updatedBy = user

        assertEquals(now, entity.createdAt)
        assertEquals(now, entity.updatedAt)
        assertEquals(user, entity.createdBy)
        assertEquals(user, entity.updatedBy)
    }

    @Test
    fun id_ShouldBeSettableAndGettable() {
        val entity = TestEntity()
        val id = 1L

        entity.id = id

        assertEquals(id, entity.id)
    }
}
