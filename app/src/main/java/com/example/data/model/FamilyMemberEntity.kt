package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val role: String, // Parent, Teen, Child, Relative, Partner
    val avatarColorHex: String, // e.g. "#3F51B5"
    val avatarIcon: String = "person", // person, face, star, favorite, etc.
    val isCurrentActiveUser: Boolean = false,
    val householdId: String = "FAM-7892-OAK",
    val monthlyContributionGoal: Double = 0.0
)
