package com.vertyll.kotlinapi.user.service

import com.vertyll.kotlinapi.auth.KeycloakIdentity
import com.vertyll.kotlinapi.common.exception.ApiException
import com.vertyll.kotlinapi.role.model.Role
import com.vertyll.kotlinapi.role.service.RoleService
import com.vertyll.kotlinapi.user.model.User
import com.vertyll.kotlinapi.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import java.util.Optional

class UserServiceTest {
    private val identity =
        KeycloakIdentity(
            keycloakId = "7d1c9a52-3a43-4c34-9a8f-2f5d7c6f1b10",
            email = "ada@kotlin-api.local",
            firstName = "Ada",
            lastName = "Lovelace",
            roles = setOf("USER", "UNKNOWN"),
        )
    private val roleService =
        mock<RoleService> { on { getOrCreateDefaultRole(any()) } doAnswer { Role(name = it.getArgument(0)) } }

    @Test
    fun `the first sign-in creates the account with the roles the application knows`() {
        val repository = savingRepository(Optional.empty())

        val user = UserService(repository, roleService).sync(identity)

        assertEquals(identity.keycloakId, user.keycloakId)
        assertEquals(setOf("USER"), user.roles)
        verify(roleService, never()).getOrCreateDefaultRole("UNKNOWN")
    }

    @Test
    fun `a later sign-in mirrors what changed in Keycloak`() {
        val existing = User(keycloakId = identity.keycloakId, firstName = "Old", lastName = "Name", email = "old@kotlin-api.local")
        val repository = savingRepository(Optional.of(existing))

        val user = UserService(repository, roleService).sync(identity.copy(roles = setOf("ADMIN")))

        assertEquals("ada@kotlin-api.local", user.email)
        assertEquals("Ada", user.firstName)
        assertEquals(setOf("ADMIN"), user.roles)
    }

    @Test
    fun `an unknown id is a 404`() {
        val repository = mock<UserRepository> { on { findById(1L) } doReturn Optional.empty() }

        val exception = assertThrows(ApiException::class.java) { UserService(repository, roleService).getUserById(1L) }

        assertEquals(HttpStatus.NOT_FOUND, exception.status)
    }

    private fun savingRepository(found: Optional<User>): UserRepository =
        mock {
            on { findByKeycloakId(identity.keycloakId) } doReturn found
            on { save(any<User>()) } doAnswer { invocation -> invocation.getArgument<User>(0).also { it.id = 1L } }
        }
}
