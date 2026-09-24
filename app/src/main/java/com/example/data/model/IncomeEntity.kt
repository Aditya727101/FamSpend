package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class IncomeCategory(val displayName: String, val iconEmoji: String) {
    SALARY("Salary", "💼"),
    FREELANCE("Freelance", "💻"),
    INVESTMENT("Investment Returns", "📈"),
    RENTAL("Rental Income", "🏠"),
    GIFT("Gift / Bonus", "🎁"),
    REFUND("Refund / Cashback", "🔄"),
    OTHER("Other Income", "💰");

    companion object {
        fun fromString(value: String): IncomeCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: SALARY
        }
    }
}

enum class IncomePaymentMethod(val displayName: String, val iconEmoji: String) {
    BANK_TRANSFER("Bank Transfer", "🏦"),
    CASH("Cash", "💵"),
    UPI("UPI", "📱"),
    CHEQUE("Cheque", "💳"),
    OTHER("Other", "🔄");

    companion object {
        fun fromString(value: String): IncomePaymentMethod {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: BANK_TRANSFER
        }
    }
}

enum class IncomeFrequency(val displayName: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly");

    companion object {
        fun fromString(value: String): IncomeFrequency {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: MONTHLY
        }
    }
}

@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val source: String,
    val category: String = IncomeCategory.SALARY.name,
    val date: Long = System.currentTimeMillis(),
    val receivedBy: String,
    val receivedByName: String = "",
    val paymentMethod: String = IncomePaymentMethod.BANK_TRANSFER.name,
    val isRecurring: Boolean = false,
    val recurringFrequency: String? = null,
    val recurringStartDate: Long? = null,
    val note: String? = null,
    val householdId: String = "FAM-1001",
    val currencySymbol: String = "₹",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
