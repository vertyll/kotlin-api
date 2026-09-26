package com.vertyll.kotlinapi.translation.service

import com.vertyll.kotlinapi.common.exception.GlobalExceptionHandler
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class DefaultTranslationsTest {
    @Test
    fun `shipped catalogue is complete and valid`() {
        val translations = DefaultTranslations.load(JsonMapper.builder().build())

        assertTrue(translations.containsKey(GlobalExceptionHandler.VALIDATION_FAILED))
        assertTrue(translations.containsKey(TranslationService.UNKNOWN_PLACEHOLDERS))
    }
}
