package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val role: String, // Parent, Teen, Child, Relative, Partner, Primary
    val avatarColorHex: String, // e.g. "#3F51B5"
    val avatarIcon: String = "person", // person, face, star, favorite, etc.
    val isCurrentActiveUser: Boolean = false,
    val householdId: String = "FAM-1001",
    val monthlyContributionGoal: Double = 0.0,
    val email: String = "",
    val userId: String = "",
    val householdRole: String = HouseholdRole.MEMBER.value, // OWNER, ADMIN, MEMBER
    val membershipStatus: String = MembershipStatus.ACTIVE.value, // ACTIVE, REMOVED
    val joinedAt: Long = 0L
) {
    fun parsedHouseholdRole(): HouseholdRole = HouseholdRole.fromString(householdRole)
    fun parsedMembershipStatus(): MembershipStatus = MembershipStatus.fromString(membershipStatus)
}
