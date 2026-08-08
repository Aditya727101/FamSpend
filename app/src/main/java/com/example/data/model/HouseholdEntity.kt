package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey
    val householdId: String = "FAM-7892-OAK",
    val householdName: String = "The Miller Family",
    val inviteCode: String = "FAM-7892-OAK",
    val defaultCurrency: String = "$",
    val totalMonthlyBudget: Double = 3500.0,
    val lastSyncedTimestamp: Long = System.currentTimeMillis(),
    val isLiveSyncEnabled: Boolean = true
)
