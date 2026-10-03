package com.vertyll.kotlinapi.user.model

import com.vertyll.kotlinapi.common.entity.BaseEntity
import com.vertyll.kotlinapi.role.model.Role
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

@Entity
@Table(name = "\"user\"")
class User(
    @Column(nullable = false, unique = true, updatable = false)
    val keycloakId: String,
    @Column(nullable = false)
    var firstName: String,
    @Column(nullable = false)
    var lastName: String,
    @Column(nullable = false, unique = true)
    var email: String,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_role",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")],
    )
    var roles: MutableSet<Role> = HashSet(),
) : BaseEntity() {
    fun syncIdentity(
        email: String,
        firstName: String,
        lastName: String,
        roles: Set<Role>,
    ) {
        this.email = email
        this.firstName = firstName
        this.lastName = lastName
        this.roles = roles.toMutableSet()
    }
}
