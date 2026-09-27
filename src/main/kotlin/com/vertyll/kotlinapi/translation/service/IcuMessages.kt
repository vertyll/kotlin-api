package com.vertyll.kotlinapi.translation.service

import com.ibm.icu.text.MessageFormat
import com.ibm.icu.text.MessagePattern

/**
 * ICU MessageFormat syntax checks. Messages are rendered by clients; the server only makes sure
 * an edited message parses and asks for no argument the code never supplies.
 */
object IcuMessages {
    fun isValid(message: String): Boolean =
        try {
            MessageFormat(message)
            true
        } catch (ignored: IllegalArgumentException) {
            false
        }

    fun placeholders(message: String): Set<String> {
        val pattern = MessagePattern(message)
        return (0 until pattern.countParts())
            .map(pattern::getPart)
            .filter { it.type == MessagePattern.Part.Type.ARG_NAME }
            .mapTo(sortedSetOf()) { pattern.getSubstring(it) }
    }
}
