package com.vertyll.kotlinapi.translation.repository

import com.vertyll.kotlinapi.translation.model.Translation
import org.springframework.data.jpa.repository.JpaRepository

interface TranslationRepository : JpaRepository<Translation, String>
