package com.vertyll.kotlinapi.role.service

import com.vertyll.kotlinapi.role.model.Role
import com.vertyll.kotlinapi.role.repository.RoleRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.util.Optional

class RoleServiceTest {
    @Test
    fun `an existing role is reused`() {
        val existing = Role(name = "ADMIN")
        val repository = mock<RoleRepository> { on { findByName("ADMIN") } doReturn Optional.of(existing) }

        assertSame(existing, RoleService(repository).getOrCreateDefaultRole("ADMIN"))
        verify(repository, never()).save(any<Role>())
    }

    @Test
    fun `a realm role seen for the first time is stored`() {
        val repository =
            mock<RoleRepository> {
                on { findByName("USER") } doReturn Optional.empty()
                on { save(any<Role>()) } doAnswer { it.getArgument(0) }
            }

        assertEquals("USER", RoleService(repository).getOrCreateDefaultRole("USER").name)
    }
}
