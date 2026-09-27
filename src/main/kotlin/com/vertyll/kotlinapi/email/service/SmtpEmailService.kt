package com.vertyll.kotlinapi.email.service

import com.vertyll.kotlinapi.config.MailProperties
import com.vertyll.kotlinapi.email.enums.EmailTemplateName
import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import org.springframework.context.annotation.Profile
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import java.nio.charset.StandardCharsets

@Service
@Profile("!test")
class SmtpEmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: SpringTemplateEngine,
    private val mailProperties: MailProperties,
) : IEmailService {
    @Async
    @Throws(MessagingException::class)
    override fun sendEmail(
        to: String,
        username: String,
        emailTemplate: EmailTemplateName,
        activationCode: String,
        subject: String,
    ) {
        val templateName = emailTemplate.name

        val mimeMessage: MimeMessage = mailSender.createMimeMessage()
        val helper =
            MimeMessageHelper(
                mimeMessage,
                MimeMessageHelper.MULTIPART_MODE_MIXED,
                StandardCharsets.UTF_8.name(),
            )

        val properties =
            mapOf(
                "username" to username,
                "activation_code" to activationCode,
            )

        val context =
            Context().apply {
                setVariables(properties)
            }

        helper.setFrom(mailProperties.from)
        helper.setTo(to)
        helper.setSubject(subject)

        val template = templateEngine.process(templateName, context)
        helper.setText(template, true)

        mailSender.send(mimeMessage)
    }
}
