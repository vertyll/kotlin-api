package com.vertyll.kotlinapi.user.controller

import com.vertyll.kotlinapi.auth.KeycloakIdentity
import com.vertyll.kotlinapi.user.dto.UserResponseDto
import com.vertyll.kotlinapi.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Accounts mirrored from Keycloak")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/me")
    @Operation(summary = "The signed-in user, synchronized from the token")
    fun me(
        @AuthenticationPrincipal jwt: Jwt,
    ): UserResponseDto = userService.sync(KeycloakIdentity.from(jwt))

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID")
    fun getUser(
        @PathVariable id: Long,
    ): UserResponseDto = userService.getUserById(id)
}
