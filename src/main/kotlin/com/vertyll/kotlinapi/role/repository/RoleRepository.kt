package com.vertyll.kotlinapi.role.repository

import com.vertyll.kotlinapi.role.model.Role
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RoleRepository : JpaRepository<Role, Long> {
    fun findByName(name: String): Optional<Role>
}
