package com.vertyll.kotlinapi.email.service

import com.vertyll.kotlinapi.email.enums.EmailTemplateName
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

/**
 * Mock implementation of the IEmailService interface for testing purposes.
 * This implementation logs email details instead of actually sending emails.
 * It's useful for development and testing environments.
 */
@Service
@Profile("test")
class MockEmailService : IEmailService {
    private val logger = LoggerFactory.getLogger(MockEmailService::class.java)

    override fun sendEmail(
        to: String,
        username: String,
        emailTemplate: EmailTemplateName,
        activationCode: String,
        subject: String,
    ) {
        logger.info("MOCK EMAIL SERVICE")
        logger.info("To: $to")
        logger.info("Username: $username")
        logger.info("Template: ${emailTemplate.name}")
        logger.info("Activation Code: $activationCode")
        logger.info("Subject: $subject")
        logger.info("Email would be sent in production environment")
    }
}
