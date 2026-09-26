package com.vertyll.kotlinapi.translation.service

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class TranslationSynchronizer(
    private val translationService: TranslationService,
    private val objectMapper: ObjectMapper,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) = translationService.synchronize(DefaultTranslations.load(objectMapper))
}
