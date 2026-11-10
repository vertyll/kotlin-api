package com.vertyll.kotlinapi.user.repository

import com.vertyll.kotlinapi.user.model.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    @EntityGraph(attributePaths = ["roles"])
    fun findByKeycloakId(keycloakId: String): Optional<User>

    @EntityGraph(attributePaths = ["roles"])
    override fun findById(id: Long): Optional<User>
}
