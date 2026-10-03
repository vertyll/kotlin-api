package com.vertyll.kotlinapi.role.service

import com.vertyll.kotlinapi.role.model.Role
import com.vertyll.kotlinapi.role.repository.RoleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RoleService(
    private val roleRepository: RoleRepository,
) {
    @Transactional
    fun getOrCreateDefaultRole(roleName: String): Role =
        roleRepository
            .findByName(roleName)
            .orElseGet { roleRepository.save(Role(name = roleName, description = "Keycloak realm role $roleName")) }
}
