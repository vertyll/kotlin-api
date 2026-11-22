package com.vertyll.kotlinapi.auth

import com.vertyll.kotlinapi.TestcontainersConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/** The CSRF cookie as the `local` profile sets it over plain HTTP, with the `test` profile's settings over the rest. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local", "test")
@Import(TestcontainersConfiguration::class)
class LocalCsrfCookieTest {
    @LocalServerPort
    private var port: Int = 0

    @Value($$"${springdoc.swagger-ui.csrf.cookie-name}")
    private lateinit var swaggerUiCookie: String

    @Test
    fun `the CSRF cookie drops the prefix and Secure`() {
        val cookies = setCookies(port)

        assertThat(cookies).noneMatch { it.startsWith("__Host-$CSRF_COOKIE=") }
        assertThat(cookies)
            .filteredOn { it.startsWith("$CSRF_COOKIE=") }
            .singleElement()
            .satisfies({ cookie ->
                assertThat(cookie)
                    .contains("; Path=/;", "; SameSite=Lax")
                    .doesNotContain("Secure", "HttpOnly", "Domain=")
            })
    }

    @Test
    fun `Swagger UI reads the CSRF cookie by its name`() {
        assertThat(swaggerUiCookie).isEqualTo(CSRF_COOKIE)
    }

    private companion object {
        private const val CSRF_COOKIE = "XSRF-TOKEN"
    }
}
