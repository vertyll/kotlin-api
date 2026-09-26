package com.vertyll.kotlinapi.translation.dto

import com.vertyll.kotlinapi.translation.model.LocalizedText
import com.vertyll.kotlinapi.translation.model.Translation
import java.time.Instant

data class TranslationResponseDto(
    val key: String,
    val messages: LocalizedText,
    val defaults: LocalizedText,
    val customized: Boolean,
    val updatedAt: Instant,
) {
    companion object {
        fun of(translation: Translation) =
            TranslationResponseDto(
                key = translation.key,
                messages = translation.messages(),
                defaults = translation.defaults(),
                customized = translation.customized,
                updatedAt = translation.updatedAt,
            )
    }
}
