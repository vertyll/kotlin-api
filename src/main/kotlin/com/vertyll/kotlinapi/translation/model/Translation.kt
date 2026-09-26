package com.vertyll.kotlinapi.translation.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * One catalogue entry: the message clients render and the shipped default it falls back to.
 * An admin's override survives a new default until it is reset.
 */
@Entity
@Table(name = "translation")
class Translation(
    @Id
    @Column(name = "message_key", nullable = false, updatable = false)
    val key: String,
    defaults: LocalizedText,
    now: Instant,
) {
    @Column(name = "message_pl", nullable = false)
    var messagePl: String = defaults.pl
        protected set

    @Column(name = "message_en", nullable = false)
    var messageEn: String = defaults.en
        protected set

    @Column(name = "default_pl", nullable = false)
    var defaultPl: String = defaults.pl
        protected set

    @Column(name = "default_en", nullable = false)
    var defaultEn: String = defaults.en
        protected set

    @Column(nullable = false)
    var customized: Boolean = false
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = now
        protected set

    fun messages(): LocalizedText = LocalizedText(messagePl, messageEn)

    fun defaults(): LocalizedText = LocalizedText(defaultPl, defaultEn)

    fun refreshDefaults(
        newDefaults: LocalizedText,
        now: Instant,
    ) {
        if (newDefaults == defaults()) {
            return
        }
        defaultPl = newDefaults.pl
        defaultEn = newDefaults.en
        if (!customized) {
            messagePl = newDefaults.pl
            messageEn = newDefaults.en
        }
        updatedAt = now
    }

    fun customize(
        messages: LocalizedText,
        now: Instant,
    ) {
        messagePl = messages.pl
        messageEn = messages.en
        customized = messages != defaults()
        updatedAt = now
    }

    fun reset(now: Instant) = customize(defaults(), now)
}
