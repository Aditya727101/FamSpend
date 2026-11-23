package com.example.data.model

enum class HouseholdRole(val value: String, val displayLabel: String) {
    OWNER("OWNER", "Owner"),
    ADMIN("ADMIN", "Admin"),
    MEMBER("MEMBER", "Member");

    fun canInviteMembers(): Boolean = this == OWNER || this == ADMIN

    fun canManageInvitations(): Boolean = this == OWNER || this == ADMIN

    fun canManageHouseholdSettings(): Boolean = this == OWNER || this == ADMIN

    fun canRemoveMember(targetRole: HouseholdRole, isSelf: Boolean): Boolean {
        if (isSelf) return false
        if (targetRole == OWNER) return false
        return when (this) {
            OWNER -> true
            ADMIN -> targetRole == MEMBER
            MEMBER -> false
        }
    }

    fun canChangeRole(targetRole: HouseholdRole, newRole: HouseholdRole, isSelf: Boolean): Boolean {
        // Users can never escalate or change their own role, and OWNER cannot be reassigned here.
        if (isSelf) return false
        if (targetRole == OWNER || newRole == OWNER) return false
        return this == OWNER && (newRole == ADMIN || newRole == MEMBER)
    }

    companion object {
        fun fromString(raw: String?): HouseholdRole {
            return entries.firstOrNull { it.value.equals(raw?.trim(), ignoreCase = true) } ?: MEMBER
        }
    }
}

enum class MembershipStatus(val value: String, val displayLabel: String) {
    ACTIVE("ACTIVE", "Active"),
    REMOVED("REMOVED", "Removed");

    companion object {
        fun fromString(raw: String?): MembershipStatus {
            return entries.firstOrNull { it.value.equals(raw?.trim(), ignoreCase = true) } ?: ACTIVE
        }
    }
}

enum class InvitationStatus(val value: String, val displayLabel: String) {
    PENDING("PENDING", "Pending"),
    ACCEPTED("ACCEPTED", "Accepted"),
    EXPIRED("EXPIRED", "Expired"),
    REVOKED("REVOKED", "Revoked");

    companion object {
        fun fromString(raw: String?): InvitationStatus {
            return entries.firstOrNull { it.value.equals(raw?.trim(), ignoreCase = true) } ?: PENDING
        }

        fun resolveEffectiveStatus(
            storedStatus: String,
            expiresAtMillis: Long,
            nowMillis: Long = System.currentTimeMillis()
        ): InvitationStatus {
            val base = fromString(storedStatus)
            return if (base == PENDING && expiresAtMillis <= nowMillis) {
                EXPIRED
            } else {
                base
            }
        }
    }
}
