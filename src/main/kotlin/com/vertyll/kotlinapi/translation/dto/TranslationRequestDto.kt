package com.vertyll.kotlinapi.translation.dto

import com.vertyll.kotlinapi.translation.model.LocalizedText
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class TranslationRequestDto(
    @field:NotBlank(message = "validation.translation.required")
    @field:Size(max = MAX_LENGTH, message = "validation.translation.tooLong")
    val pl: String,
    @field:NotBlank(message = "validation.translation.required")
    @field:Size(max = MAX_LENGTH, message = "validation.translation.tooLong")
    val en: String,
) {
    companion object {
        const val MAX_LENGTH = 2000
    }

    fun toLocalizedText() = LocalizedText(pl, en)
}
