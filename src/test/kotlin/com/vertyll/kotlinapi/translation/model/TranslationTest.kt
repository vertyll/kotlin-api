package com.vertyll.kotlinapi.translation.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class TranslationTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `new default replaces an untouched message`() {
        val translation = Translation("key", LocalizedText("A", "B"), now)

        translation.refreshDefaults(LocalizedText("C", "D"), now)

        assertEquals(LocalizedText("C", "D"), translation.messages())
    }

    @Test
    fun `override survives a new default until reset`() {
        val translation = Translation("key", LocalizedText("A", "B"), now)
        translation.customize(LocalizedText("X", "Y"), now)

        translation.refreshDefaults(LocalizedText("C", "D"), now)
        assertTrue(translation.customized)
        assertEquals(LocalizedText("X", "Y"), translation.messages())

        translation.reset(now)
        assertFalse(translation.customized)
        assertEquals(LocalizedText("C", "D"), translation.messages())
    }

    @Test
    fun `customizing back to the default is not an override`() {
        val translation = Translation("key", LocalizedText("A", "B"), now)

        translation.customize(LocalizedText("A", "B"), now)

        assertFalse(translation.customized)
    }
}
