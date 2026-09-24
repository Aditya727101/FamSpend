package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptShareHelper {

    fun shareExpenseReceipt(context: Context, expense: ExpenseEntity, currencySymbol: String) {
        val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val formattedDate = sdf.format(Date(expense.timestamp))
        val formattedAmount = "${currencySymbol}${String.format(Locale.US, "%.2f", expense.amount)}"

        val shareText = buildString {
            appendLine("🧾 FAMSPEND RECEIPT")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Title: ${expense.description}")
            appendLine("Amount: $formattedAmount")
            appendLine("Category: ${expense.category}")
            appendLine("Paid by: ${expense.paidByMemberName}")
            appendLine("Payment Method: ${expense.paymentMethod}")
            appendLine("Split: ${expense.splitType}")
            appendLine("Date: $formattedDate")
            if (expense.note.isNotBlank()) {
                appendLine("Note: ${expense.note}")
            }
            if (expense.isSettled) {
                appendLine("Status: ✅ Settled")
            } else if (expense.splitType != "Paid Individual") {
                appendLine("Status: ⏳ Pending settlement")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Shared via FamSpend")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Receipt")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun sendSettlementReminder(context: Context, expense: ExpenseEntity, currencySymbol: String, perPersonAmount: Double) {
        val shareText = buildString {
            appendLine("👋 Hey! Friendly reminder for ${expense.description}.")
            appendLine("Total: ${currencySymbol}${String.format(Locale.US, "%.2f", expense.amount)}")
            appendLine("Your share: ${currencySymbol}${String.format(Locale.US, "%.2f", perPersonAmount)}")
            appendLine("Paid by ${expense.paidByMemberName}. Please settle when convenient!")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Send Reminder")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
