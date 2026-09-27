package com.vertyll.kotlinapi.email.service

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.vertyll.kotlinapi.email.enums.EmailTemplateName
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class MockEmailServiceTest {
    private lateinit var mockEmailService: MockEmailService
    private lateinit var listAppender: ListAppender<ILoggingEvent>
    private lateinit var logger: Logger

    @BeforeEach
    fun setUp() {
        mockEmailService = MockEmailService()

        logger = LoggerFactory.getLogger(MockEmailService::class.java) as Logger
        listAppender = ListAppender()
        listAppender.start()
        logger.addAppender(listAppender)
    }

    @AfterEach
    fun tearDown() {
        logger.detachAppender(listAppender)
    }

    @Test
    fun `sendEmail should log all email details`() {
        val to = "test@example.com"
        val username = "Test User"
        val emailTemplate = EmailTemplateName.ACTIVATE_ACCOUNT
        val activationCode = "123456"
        val subject = "Test Subject"

        mockEmailService.sendEmail(to, username, emailTemplate, activationCode, subject)

        val logsList = listAppender.list

        assertTrue(logsList.any { it.message.contains("MOCK EMAIL SERVICE") })
        assertTrue(logsList.any { it.message.contains("To: $to") })
        assertTrue(logsList.any { it.message.contains("Username: $username") })
        assertTrue(logsList.any { it.message.contains("Template: ${emailTemplate.name}") })
        assertTrue(logsList.any { it.message.contains("Activation Code: $activationCode") })
        assertTrue(logsList.any { it.message.contains("Subject: $subject") })
        assertTrue(logsList.any { it.message.contains("Email would be sent in production environment") })
    }
}
