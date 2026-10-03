package com.vertyll.kotlinapi.user.repository

import com.vertyll.kotlinapi.user.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByKeycloakId(keycloakId: String): Optional<User>
}
