package com.vertyll.kotlinapi.translation.service

import com.vertyll.kotlinapi.translation.model.Language
import com.vertyll.kotlinapi.translation.model.LocalizedText
import org.springframework.core.io.ClassPathResource
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper

/**
 * The catalogue shipped on the classpath (`i18n/{pl,en}.json`), the source of every default.
 * Both languages must list the same keys and every message must parse.
 */
object DefaultTranslations {
    private val MESSAGES = object : TypeReference<Map<String, String>>() {}

    fun load(objectMapper: ObjectMapper): Map<String, LocalizedText> {
        val byLanguage = Language.entries.associateWith { read(objectMapper, it) }
        val keys = byLanguage.getValue(Language.PL).keys.toSortedSet()
        byLanguage.forEach { (language, messages) ->
            check(messages.keys == keys) {
                "Translations ${language.code} differ in keys: ${(keys - messages.keys) + (messages.keys - keys)}"
            }
        }
        return keys.associateWithTo(sortedMapOf()) { key ->
            val text =
                LocalizedText(
                    pl = byLanguage.getValue(Language.PL).getValue(key),
                    en = byLanguage.getValue(Language.EN).getValue(key),
                )
            Language.entries.forEach { language ->
                check(IcuMessages.isValid(text.of(language))) { "Invalid ICU message $key (${language.code})" }
            }
            text
        }
    }

    private fun read(
        objectMapper: ObjectMapper,
        language: Language,
    ): Map<String, String> =
        ClassPathResource("i18n/${language.code}.json").inputStream.use {
            objectMapper.readValue(it, MESSAGES)
        }
}
