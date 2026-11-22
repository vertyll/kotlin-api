package com.vertyll.kotlinapi.auth

import com.vertyll.kotlinapi.TestcontainersConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/** The CSRF cookie as a browser receives it, from the embedded server, with the secure cookies of production. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class CsrfCookieTest {
    @LocalServerPort
    private var port: Int = 0

    @Value($$"${springdoc.swagger-ui.csrf.cookie-name}")
    private lateinit var swaggerUiCookie: String

    @Test
    fun `the CSRF cookie is bound to this host and readable by same-site scripts over HTTPS only`() {
        assertThat(setCookies(port))
            .filteredOn { it.startsWith("$CSRF_COOKIE=") }
            .singleElement()
            .satisfies({ cookie ->
                assertThat(cookie)
                    .contains("; Path=/;", "; Secure", "; SameSite=Lax")
                    .doesNotContain("HttpOnly", "Domain=")
            })
    }

    @Test
    fun `Swagger UI reads the CSRF cookie by its name`() {
        assertThat(swaggerUiCookie).isEqualTo(CSRF_COOKIE)
    }

    private companion object {
        private const val CSRF_COOKIE = "__Host-XSRF-TOKEN"
    }
}

internal fun setCookies(port: Int): List<String> =
    HttpClient.newHttpClient().use { http ->
        http
            .send(
                HttpRequest.newBuilder(URI.create("http://localhost:$port/api/v1/auth/session")).build(),
                HttpResponse.BodyHandlers.discarding(),
            ).headers()
            .allValues(HttpHeaders.SET_COOKIE)
    }
