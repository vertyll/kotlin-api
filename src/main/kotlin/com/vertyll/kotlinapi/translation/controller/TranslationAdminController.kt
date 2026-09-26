package com.vertyll.kotlinapi.translation.controller

import com.vertyll.kotlinapi.translation.dto.TranslationRequestDto
import com.vertyll.kotlinapi.translation.dto.TranslationResponseDto
import com.vertyll.kotlinapi.translation.service.TranslationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/translations")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Translations admin", description = "Catalogue management")
class TranslationAdminController(
    private val translationService: TranslationService,
) {
    @GetMapping
    @Operation(summary = "List every translation with its default")
    fun findAll(): List<TranslationResponseDto> = translationService.findAll()

    @PutMapping("/{key}")
    @Operation(summary = "Override a message")
    fun update(
        @PathVariable key: String,
        @RequestBody @Valid request: TranslationRequestDto,
    ): TranslationResponseDto = translationService.update(key, request.toLocalizedText())

    @DeleteMapping("/{key}/customization")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Restore the shipped default")
    fun reset(
        @PathVariable key: String,
    ) = translationService.reset(key)
}
