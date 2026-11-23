package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "invitations",
    indices = [
        Index(value = ["householdId"]),
        Index(value = ["inviteeEmail"]),
        Index(value = ["invitationTokenHash"], unique = true),
        Index(value = ["status"])
    ]
)
data class InvitationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val householdId: String,
    val householdName: String,
    val inviterUserId: String,
    val inviterName: String,
    val inviteeEmail: String = "",
    val inviteeName: String = "",
    val personalMessage: String = "",
    val invitationTokenHash: String,
    val role: String = HouseholdRole.MEMBER.value,
    val status: String = InvitationStatus.PENDING.value,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val acceptedAt: Long? = null,
    val acceptedByUserId: String = "",
    val revokedAt: Long? = null
) {
    fun effectiveStatus(nowMillis: Long = System.currentTimeMillis()): InvitationStatus {
        return InvitationStatus.resolveEffectiveStatus(status, expiresAt, nowMillis)
    }

    fun isLinkOnlyInvitation(): Boolean = inviteeEmail.isBlank()
}
