package com.vertyll.kotlinapi.translation.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IcuMessagesTest {
    @Test
    fun `accepts plural and collects nested arguments`() {
        val message = "{max, plural, one {# znak} few {# znaki} many {# znaków} other {# znaku}} dla {name}"

        assertTrue(IcuMessages.isValid(message))
        assertEquals(setOf("max", "name"), IcuMessages.placeholders(message))
    }

    @Test
    fun `rejects broken syntax`() {
        assertFalse(IcuMessages.isValid("{max, plural, one {# znak}"))
    }
}
