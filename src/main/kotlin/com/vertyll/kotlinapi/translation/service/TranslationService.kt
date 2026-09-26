package com.vertyll.kotlinapi.translation.service

import com.vertyll.kotlinapi.common.exception.ApiException
import com.vertyll.kotlinapi.translation.dto.TranslationResponseDto
import com.vertyll.kotlinapi.translation.model.Language
import com.vertyll.kotlinapi.translation.model.LocalizedText
import com.vertyll.kotlinapi.translation.model.Translation
import com.vertyll.kotlinapi.translation.repository.TranslationRepository
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class TranslationService(
    private val repository: TranslationRepository,
    private val clock: Clock,
) {
    companion object {
        const val TRANSLATION_NOT_FOUND = "errors.translation.notFound"
        const val LANGUAGE_NOT_FOUND = "errors.translation.languageNotFound"
        const val INVALID_MESSAGE = "errors.translation.invalidMessage"
        const val UNKNOWN_PLACEHOLDERS = "errors.translation.unknownPlaceholders"
    }

    @Transactional(readOnly = true)
    fun messages(languageCode: String): Map<String, String> {
        val language =
            Language.fromCode(languageCode) ?: throw ApiException(LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND)
        return repository.findAll().associateTo(sortedMapOf()) { it.key to it.messages().of(language) }
    }

    @Transactional(readOnly = true)
    fun findAll(): List<TranslationResponseDto> = repository.findAll(Sort.by("key")).map(TranslationResponseDto::of)

    @Transactional
    fun update(
        key: String,
        messages: LocalizedText,
    ): TranslationResponseDto {
        val translation = find(key)
        Language.entries.forEach { language ->
            val message = messages.of(language)
            if (!IcuMessages.isValid(message)) {
                throw ApiException(INVALID_MESSAGE, HttpStatus.BAD_REQUEST, mapOf("language" to language.code))
            }
            val unknown = IcuMessages.placeholders(message) - IcuMessages.placeholders(translation.defaults().of(language))
            if (unknown.isNotEmpty()) {
                throw ApiException(
                    UNKNOWN_PLACEHOLDERS,
                    HttpStatus.BAD_REQUEST,
                    mapOf("language" to language.code, "placeholders" to unknown.joinToString(", ")),
                )
            }
        }
        translation.customize(messages, Instant.now(clock))
        return TranslationResponseDto.of(translation)
    }

    @Transactional
    fun reset(key: String) = find(key).reset(Instant.now(clock))

    /**
     * Brings the stored catalogue in line with the shipped defaults: adds new keys, adopts changed
     * defaults and drops keys the code no longer uses.
     */
    @Transactional
    fun synchronize(defaults: Map<String, LocalizedText>) {
        val now = Instant.now(clock)
        val existing = repository.findAll().associateByTo(mutableMapOf()) { it.key }
        defaults.forEach { (key, text) ->
            val translation = existing.remove(key)
            if (translation == null) {
                repository.save(Translation(key, text, now))
            } else {
                translation.refreshDefaults(text, now)
            }
        }
        repository.deleteAll(existing.values)
    }

    private fun find(key: String): Translation =
        repository.findById(key).orElseThrow { ApiException(TRANSLATION_NOT_FOUND, HttpStatus.NOT_FOUND) }
}
