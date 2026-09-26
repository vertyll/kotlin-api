package com.vertyll.kotlinapi.translation.model

enum class Language(
    val code: String,
) {
    PL("pl"),
    EN("en"),
    ;

    companion object {
        fun fromCode(code: String): Language? = entries.firstOrNull { it.code == code }
    }
}
