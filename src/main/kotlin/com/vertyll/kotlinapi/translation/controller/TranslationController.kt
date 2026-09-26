package com.vertyll.kotlinapi.translation.controller

import com.vertyll.kotlinapi.translation.service.TranslationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/translations")
@Tag(name = "Translations", description = "ICU MessageFormat catalogue for clients")
class TranslationController(
    private val translationService: TranslationService,
) {
    @GetMapping("/{language}")
    @Operation(summary = "Get every message of one language, keyed by message key")
    fun messages(
        @PathVariable language: String,
    ): Map<String, String> = translationService.messages(language)
}
