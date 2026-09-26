package com.vertyll.kotlinapi.translation.service

import com.vertyll.kotlinapi.common.exception.ApiException
import com.vertyll.kotlinapi.translation.model.LocalizedText
import com.vertyll.kotlinapi.translation.model.Translation
import com.vertyll.kotlinapi.translation.repository.TranslationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional

class TranslationServiceTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")
    private val repository: TranslationRepository = mock()
    private val service = TranslationService(repository, Clock.fixed(now, ZoneOffset.UTC))

    private fun stored(
        key: String,
        pl: String,
        en: String,
    ) = Translation(key, LocalizedText(pl, en), now)

    @Test
    fun `messages returns one language keyed by message key`() {
        whenever(repository.findAll()).thenReturn(listOf(stored("b", "B", "b"), stored("a", "A", "a")))

        assertEquals(mapOf("a" to "a", "b" to "b"), service.messages("en"))
    }

    @Test
    fun `unknown language is not found`() {
        val ex = assertThrows<ApiException> { service.messages("de") }

        assertEquals(TranslationService.LANGUAGE_NOT_FOUND, ex.messageKey)
    }

    @Test
    fun `update rejects invalid ICU and unknown arguments`() {
        whenever(repository.findById("greeting")).thenReturn(Optional.of(stored("greeting", "Cześć {name}", "Hi {name}")))

        val invalid = assertThrows<ApiException> { service.update("greeting", LocalizedText("{name", "Hi")) }
        assertEquals(TranslationService.INVALID_MESSAGE, invalid.messageKey)
        assertEquals(mapOf("language" to "pl"), invalid.args)

        val unknown = assertThrows<ApiException> { service.update("greeting", LocalizedText("Hej", "Hi {user}")) }
        assertEquals(TranslationService.UNKNOWN_PLACEHOLDERS, unknown.messageKey)
        assertEquals(mapOf("language" to "en", "placeholders" to "user"), unknown.args)
    }

    @Test
    fun `update stores an override`() {
        whenever(repository.findById("greeting")).thenReturn(Optional.of(stored("greeting", "Cześć {name}", "Hi {name}")))

        val response = service.update("greeting", LocalizedText("Witaj {name}", "Hello {name}"))

        assertTrue(response.customized)
        assertEquals(LocalizedText("Witaj {name}", "Hello {name}"), response.messages)
    }

    @Test
    fun `synchronize adds new keys and drops obsolete ones`() {
        val obsolete = stored("old", "O", "O")
        whenever(repository.findAll()).thenReturn(listOf(obsolete))

        service.synchronize(mapOf("new" to LocalizedText("N", "N")))

        val saved = argumentCaptor<Translation>()
        verify(repository).save(saved.capture())
        assertEquals("new", saved.firstValue.key)
        val deleted = argumentCaptor<Iterable<Translation>>()
        verify(repository).deleteAll(deleted.capture())
        assertEquals(listOf(obsolete), deleted.firstValue.toList())
    }

    @Test
    fun `unknown key is not found`() {
        whenever(repository.findById(any())).thenReturn(Optional.empty())

        val ex = assertThrows<ApiException> { service.reset("missing") }

        assertEquals(TranslationService.TRANSLATION_NOT_FOUND, ex.messageKey)
    }
}
