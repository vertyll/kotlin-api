package com.vertyll.kotlinapi.email.service

import com.vertyll.kotlinapi.config.MailProperties
import com.vertyll.kotlinapi.email.enums.EmailTemplateName
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.util.ReflectionTestUtils
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

@ExtendWith(MockitoExtension::class)
class SmtpEmailServiceTest {
    @Mock
    private lateinit var mailSender: JavaMailSender

    @Mock
    private lateinit var templateEngine: SpringTemplateEngine

    @Mock
    private lateinit var mimeMessage: MimeMessage

    @Mock
    private lateinit var mailProperties: MailProperties

    @InjectMocks
    private lateinit var emailService: SmtpEmailService

    @Captor
    private lateinit var contextCaptor: ArgumentCaptor<Context>

    @Captor
    private lateinit var templateNameCaptor: ArgumentCaptor<String>

    @BeforeEach
    fun setUp() {
        // Mock mailProperties to return a valid 'from' email
        `when`(mailProperties.from).thenReturn("sender@example.com")

        // Mock the createMimeMessage method to return our mock MimeMessage
        `when`(mailSender.createMimeMessage()).thenReturn(mimeMessage)

        // Mock the process method to return a template
        `when`(templateEngine.process(anyString(), any(Context::class.java))).thenReturn("<html>Test Template</html>")
    }

    @Test
    fun `sendEmail should send email with correct parameters`() {
        // given
        val to = "recipient@example.com"
        val username = "Test User"
        val emailTemplate = EmailTemplateName.ACTIVATE_ACCOUNT
        val activationCode = "123456"
        val subject = "Test Subject"

        // when
        emailService.sendEmail(to, username, emailTemplate, activationCode, subject)

        // then
        // Verify createMimeMessage was called
        verify(mailSender).createMimeMessage()

        // Verify process was called with correct template name and context
        verify(templateEngine).process(templateNameCaptor.capture(), contextCaptor.capture())

        // Verify template name
        assert(templateNameCaptor.value == emailTemplate.name)

        // Verify context variables
        val capturedContext = contextCaptor.value
        val variables = ReflectionTestUtils.getField(capturedContext, "variables") as Map<*, *>
        assert(variables["username"] == username)
        assert(variables["activation_code"] == activationCode)

        // Verify send was called with our mock MimeMessage
        verify(mailSender).send(mimeMessage)
    }
}
