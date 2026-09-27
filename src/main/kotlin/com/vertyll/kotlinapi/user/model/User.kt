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
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(name = "\"user\"")
class User(
    @Column(nullable = false)
    var firstName: String,
    @Column(nullable = false)
    var lastName: String,
    @Column(nullable = false, unique = true)
    private var email: String,
    @Column(nullable = false)
    private var password: String,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_role",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")],
    )
    var roles: MutableSet<Role> = HashSet(),
    @Column(nullable = false)
    var enabled: Boolean = false,
) : BaseEntity(),
    UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> = roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }

    override fun getUsername(): String = email

    override fun getPassword(): String = password

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = enabled

    companion object {
        fun create(
            firstName: String,
            lastName: String,
            email: String,
            password: String,
            roles: MutableSet<Role> = HashSet(),
            enabled: Boolean = false,
        ): User =
            User(
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password,
                roles = roles,
                enabled = enabled,
            )
    }
}
