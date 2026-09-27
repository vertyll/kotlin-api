package com.vertyll.kotlinapi.email.service

import com.vertyll.kotlinapi.email.enums.EmailTemplateName
import jakarta.mail.MessagingException

fun interface IEmailService {
    @Throws(MessagingException::class)
    fun sendEmail(
        to: String,
        username: String,
        emailTemplate: EmailTemplateName,
        activationCode: String,
        subject: String,
    )
}
