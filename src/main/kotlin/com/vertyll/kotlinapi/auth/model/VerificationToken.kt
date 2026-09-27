package com.vertyll.kotlinapi.auth.model

import com.vertyll.kotlinapi.auth.enums.VerificationTokenType
import com.vertyll.kotlinapi.common.entity.BaseEntity
import com.vertyll.kotlinapi.user.model.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "verification_token")
class VerificationToken(
    @Column(nullable = false)
    var token: String = "",
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,
    @Column(nullable = false)
    var expiryDate: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false)
    var used: Boolean = false,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var tokenType: VerificationTokenType = VerificationTokenType.ACCOUNT_ACTIVATION,
    /**
     * Additional data that might be needed for specific token types.
     * For example, for EMAIL_CHANGE, this could store the new email address.
     */
    @Column(nullable = true)
    var additionalData: String? = null,
) : BaseEntity()
