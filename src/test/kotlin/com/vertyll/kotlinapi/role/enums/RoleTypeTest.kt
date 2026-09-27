package com.vertyll.kotlinapi.role.enums

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoleTypeTest {
    @Test
    fun `enum should have correct number of values`() {
        val values = RoleType.entries

        assertEquals(4, values.size)
    }

    @Test
    fun `enum should contain expected values`() {
        assertTrue(RoleType.entries.contains(RoleType.ADMIN))
        assertTrue(RoleType.entries.contains(RoleType.USER))
        assertTrue(RoleType.entries.contains(RoleType.MANAGER))
        assertTrue(RoleType.entries.contains(RoleType.EMPLOYEE))
    }

    @Test
    fun `getAuthority should return correct authority string`() {
        assertEquals("ROLE_ADMIN", RoleType.ADMIN.getAuthority())
        assertEquals("ROLE_USER", RoleType.USER.getAuthority())
        assertEquals("ROLE_MANAGER", RoleType.MANAGER.getAuthority())
        assertEquals("ROLE_EMPLOYEE", RoleType.EMPLOYEE.getAuthority())
    }

    @Test
    fun `valueOf should return correct enum value`() {
        assertSame(RoleType.ADMIN, RoleType.valueOf("ADMIN"))
        assertSame(RoleType.USER, RoleType.valueOf("USER"))
        assertSame(RoleType.MANAGER, RoleType.valueOf("MANAGER"))
        assertSame(RoleType.EMPLOYEE, RoleType.valueOf("EMPLOYEE"))
    }
}
