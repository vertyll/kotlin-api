package com.vertyll.kotlinapi.user.service

import com.vertyll.kotlinapi.common.exception.ApiException
import com.vertyll.kotlinapi.role.model.Role
import com.vertyll.kotlinapi.role.service.RoleService
import com.vertyll.kotlinapi.user.dto.UserCreateDto
import com.vertyll.kotlinapi.user.dto.UserResponseDto
import com.vertyll.kotlinapi.user.dto.UserUpdateDto
import com.vertyll.kotlinapi.user.model.User
import com.vertyll.kotlinapi.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleService: RoleService,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun createUser(dto: UserCreateDto): UserResponseDto {
        if (userRepository.existsByEmail(dto.email)) {
            throw ApiException("errors.user.emailTaken", HttpStatus.BAD_REQUEST)
        }

        val roles = mutableSetOf<Role>()
        if (dto.roleNames.isNotEmpty()) {
            dto.roleNames.forEach { roleName ->
                roles.add(roleService.getOrCreateDefaultRole(roleName))
            }
        } else {
            roles.add(roleService.getOrCreateDefaultRole("USER"))
        }

        val user =
            User.create(
                firstName = dto.firstName,
                lastName = dto.lastName,
                email = dto.email,
                password = requireNotNull(passwordEncoder.encode(dto.password)) { "Password encoding failed" },
                roles = roles,
                enabled = true,
            )

        val savedUser = userRepository.save(user)
        return mapToDto(savedUser)
    }

    @Transactional
    fun updateUser(
        id: Long,
        dto: UserUpdateDto,
    ): UserResponseDto {
        val user =
            userRepository
                .findById(id)
                .orElseThrow { ApiException("errors.user.notFound", HttpStatus.NOT_FOUND) }

        dto.firstName.let { user.firstName = it }
        dto.lastName.let { user.lastName = it }

        dto.email.let {
            val updatedUser =
                User.create(
                    firstName = user.firstName,
                    lastName = user.lastName,
                    email = it,
                    password = user.password,
                    roles = user.roles,
                    enabled = user.isEnabled,
                )
            updatedUser.id = user.id
            return mapToDto(userRepository.save(updatedUser))
        }
    }

    fun getUserById(id: Long): UserResponseDto {
        val user =
            userRepository
                .findById(id)
                .orElseThrow { ApiException("errors.user.notFound", HttpStatus.NOT_FOUND) }
        return mapToDto(user)
    }

    private fun mapToDto(user: User): UserResponseDto =
        UserResponseDto(
            id = checkNotNull(user.id),
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.username,
            roles = user.roles.map { it.name }.toSet(),
            enabled = user.isEnabled,
        )
}
