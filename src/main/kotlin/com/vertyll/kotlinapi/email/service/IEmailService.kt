package com.vertyll.kotlinapi.email.service

import com.vertyll.kotlinapi.email.enums.EmailTemplateName
import jakarta.mail.MessagingException

/**
 * Interface for email services.
 * This interface defines the contract for sending emails in the application.
 * Implementations can use different email providers or strategies.
 */
fun interface IEmailService {
    /**
     * Sends an email to the specified recipient.
     *
     * @param to The email address of the recipient
     * @param username The username or name of the recipient
     * @param emailTemplate The template to use for the email content
     * @param activationCode The activation code to include in the email
     * @param subject The subject line of the email
     * @throws MessagingException If there's an error sending the email
     */
    @Throws(MessagingException::class)
    fun sendEmail(
        to: String,
        username: String,
        emailTemplate: EmailTemplateName,
        activationCode: String,
        subject: String,
    )
}
