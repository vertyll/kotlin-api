package com.vertyll.kotlinapi.auth.enums

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VerificationTokenTypeTest {
    @Test
    fun `enum should have correct number of values`() {
        val values = VerificationTokenType.entries

        assertEquals(4, values.size)
    }

    @Test
    fun `enum should contain expected values`() {
        assertTrue(VerificationTokenType.entries.contains(VerificationTokenType.ACCOUNT_ACTIVATION))
        assertTrue(VerificationTokenType.entries.contains(VerificationTokenType.EMAIL_CHANGE))
        assertTrue(VerificationTokenType.entries.contains(VerificationTokenType.PASSWORD_CHANGE))
        assertTrue(VerificationTokenType.entries.contains(VerificationTokenType.PASSWORD_RESET))
    }

    @Test
    fun `valueOf should return correct enum value`() {
        assertSame(VerificationTokenType.ACCOUNT_ACTIVATION, VerificationTokenType.valueOf("ACCOUNT_ACTIVATION"))
        assertSame(VerificationTokenType.EMAIL_CHANGE, VerificationTokenType.valueOf("EMAIL_CHANGE"))
        assertSame(VerificationTokenType.PASSWORD_CHANGE, VerificationTokenType.valueOf("PASSWORD_CHANGE"))
        assertSame(VerificationTokenType.PASSWORD_RESET, VerificationTokenType.valueOf("PASSWORD_RESET"))
    }
}
