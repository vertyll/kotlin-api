package com.vertyll.kotlinapi.user.service

import com.vertyll.kotlinapi.auth.KeycloakIdentity
import com.vertyll.kotlinapi.common.exception.ApiException
import com.vertyll.kotlinapi.role.enums.RoleType
import com.vertyll.kotlinapi.role.service.RoleService
import com.vertyll.kotlinapi.user.dto.UserResponseDto
import com.vertyll.kotlinapi.user.model.User
import com.vertyll.kotlinapi.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleService: RoleService,
) {
    @Transactional
    fun sync(identity: KeycloakIdentity): UserResponseDto {
        val roles =
            identity.roles
                .filter { role -> RoleType.entries.any { it.name == role } }
                .map(roleService::getOrCreateDefaultRole)
                .toSet()
        val user =
            userRepository
                .findByKeycloakId(identity.keycloakId)
                .orElseGet {
                    User(
                        keycloakId = identity.keycloakId,
                        firstName = identity.firstName,
                        lastName = identity.lastName,
                        email = identity.email,
                    )
                }
        user.syncIdentity(identity.email, identity.firstName, identity.lastName, roles)
        return mapToDto(userRepository.save(user))
    }

    @Transactional(readOnly = true)
    fun getUserById(id: Long): UserResponseDto =
        userRepository
            .findById(id)
            .map(::mapToDto)
            .orElseThrow { ApiException("errors.user.notFound", HttpStatus.NOT_FOUND) }

    private fun mapToDto(user: User): UserResponseDto =
        UserResponseDto(
            id = checkNotNull(user.id),
            keycloakId = user.keycloakId,
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            roles = user.roles.map { it.name }.toSet(),
        )
}
