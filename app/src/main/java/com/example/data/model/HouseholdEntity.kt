package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey
    val householdId: String = "FAM-1001",
    val householdName: String = "My Household",
    val inviteCode: String = "FAM-1001",
    val defaultCurrency: String = "₹",
    val totalMonthlyBudget: Double = 3000.0,
    val lastSyncedTimestamp: Long = System.currentTimeMillis(),
    val isLiveSyncEnabled: Boolean = true
)
