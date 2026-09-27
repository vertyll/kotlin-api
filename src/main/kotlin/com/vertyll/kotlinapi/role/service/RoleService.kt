package com.vertyll.kotlinapi.role.service

import com.vertyll.kotlinapi.common.exception.ApiException
import com.vertyll.kotlinapi.role.dto.RoleCreateDto
import com.vertyll.kotlinapi.role.dto.RoleResponseDto
import com.vertyll.kotlinapi.role.dto.RoleUpdateDto
import com.vertyll.kotlinapi.role.model.Role
import com.vertyll.kotlinapi.role.repository.RoleRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RoleService(
    private val roleRepository: RoleRepository,
) {
    @Transactional
    fun createRole(dto: RoleCreateDto): RoleResponseDto {
        if (roleRepository.existsByName(dto.name)) {
            throw ApiException("errors.role.alreadyExists", HttpStatus.BAD_REQUEST)
        }

        val role =
            Role(
                name = dto.name,
                description = dto.description,
            )

        val savedRole = roleRepository.save(role)
        return mapToDto(savedRole)
    }

    @Transactional
    fun updateRole(
        id: Long,
        dto: RoleUpdateDto,
    ): RoleResponseDto {
        val role =
            roleRepository
                .findById(id)
                .orElseThrow { ApiException("errors.role.notFound", HttpStatus.NOT_FOUND) }

        if (roleRepository.existsByName(dto.name) && role.name != dto.name) {
            throw ApiException("errors.role.alreadyExists", HttpStatus.BAD_REQUEST)
        }

        val updatedRole =
            role.copy(
                name = dto.name,
                description = dto.description,
            )

        val savedRole = roleRepository.save(updatedRole)
        return mapToDto(savedRole)
    }

    fun getOrCreateDefaultRole(roleName: String): Role =
        roleRepository
            .findByName(roleName)
            .orElseGet {
                val role =
                    Role(
                        name = roleName,
                        description = "Default role: $roleName",
                    )
                roleRepository.save(role)
            }

    fun getRoleById(id: Long): RoleResponseDto {
        val role =
            roleRepository
                .findById(id)
                .orElseThrow { ApiException("errors.role.notFound", HttpStatus.NOT_FOUND) }
        return mapToDto(role)
    }

    private fun mapToDto(role: Role): RoleResponseDto =
        RoleResponseDto(
            id = checkNotNull(role.id),
            name = role.name,
            description = role.description,
        )
}
