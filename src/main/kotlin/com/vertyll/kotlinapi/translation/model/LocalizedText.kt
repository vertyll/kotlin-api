package com.vertyll.kotlinapi.translation.model

data class LocalizedText(
    val pl: String,
    val en: String,
) {
    fun of(language: Language): String =
        when (language) {
            Language.PL -> pl
            Language.EN -> en
        }
}
