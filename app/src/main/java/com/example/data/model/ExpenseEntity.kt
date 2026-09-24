package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val amount: Double,
    val currencySymbol: String = "₹",
    val category: String, // Groceries, Dining, Utilities, Transport, Entertainment, etc.
    val description: String,
    val paidByMemberId: String,
    val paidByMemberName: String,
    val splitType: String = "Paid Individual", // Paid Individual, Split Equally, Split Custom
    val timestamp: Long = System.currentTimeMillis(),
    val householdId: String = "FAM-1001",
    val paymentMethod: String = "Credit Card", // Cash, Credit Card, Debit, Bank Transfer, Google Pay
    val note: String = "",
    val isRecurring: Boolean = false,
    val recurringFrequency: String = "Monthly", // "Monthly", "Weekly", "Yearly"
    val recurringDayOfMonth: Int = 1, // Day 1..31
    val isAutoCreated: Boolean = false,
    val isSettled: Boolean = false,
    val receiptUri: String? = null,
    val tags: String = ""
)

