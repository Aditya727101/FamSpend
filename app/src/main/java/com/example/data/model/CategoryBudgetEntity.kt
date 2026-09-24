package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_budgets")
data class CategoryBudgetEntity(
    @PrimaryKey
    val categoryName: String,
    val monthlyLimit: Double,
    val householdId: String = "FAM-1001",
    val iconName: String = "category",
    val colorHex: String = "#3F51B5"
)
