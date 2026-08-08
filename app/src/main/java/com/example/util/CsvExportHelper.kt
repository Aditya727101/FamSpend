package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.ExpenseEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CsvExportHelper {

    /**
     * Filters expenses belonging to the current month and year.
     */
    fun filterCurrentMonthExpenses(expenses: List<ExpenseEntity>): List<ExpenseEntity> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return expenses.filter { expense ->
            val expCal = Calendar.getInstance().apply { timeInMillis = expense.timestamp }
            expCal.get(Calendar.MONTH) == currentMonth && expCal.get(Calendar.YEAR) == currentYear
        }.sortedByDescending { it.timestamp }
    }

    /**
     * Formats current month's expenses list into a CSV structured string.
     */
    fun generateCsvString(expenses: List<ExpenseEntity>, householdName: String = "Household"): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val currentMonthName = monthFormat.format(Date())

        val sb = StringBuilder()
        // Header info metadata
        sb.append("# ").append(householdName).append(" Expense Report - ").append(currentMonthName).append("\n")
        sb.append("# Exported on: ").append(dateFormat.format(Date())).append(" ").append(timeFormat.format(Date())).append("\n")
        sb.append("# Total Transactions: ").append(expenses.size).append("\n")
        val totalSum = expenses.sumOf { it.amount }
        sb.append("# Total Spent: ").append(String.format(Locale.US, "%.2f", totalSum)).append("\n\n")

        // Column CSV Headers
        sb.append("ID,Date,Time,Category,Description,Amount,Currency,Paid By,Payment Method,Split Type,Recurring,Notes\n")

        for (expense in expenses) {
            val dateStr = dateFormat.format(Date(expense.timestamp))
            val timeStr = timeFormat.format(Date(expense.timestamp))

            sb.append(escapeCsv(expense.id.toString())).append(",")
            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(escapeCsv(timeStr)).append(",")
            sb.append(escapeCsv(expense.category)).append(",")
            sb.append(escapeCsv(expense.description)).append(",")
            sb.append(String.format(Locale.US, "%.2f", expense.amount)).append(",")
            sb.append(escapeCsv(expense.currencySymbol)).append(",")
            sb.append(escapeCsv(expense.paidByMemberName)).append(",")
            sb.append(escapeCsv(expense.paymentMethod)).append(",")
            sb.append(escapeCsv(expense.splitType)).append(",")
            sb.append(escapeCsv(if (expense.isRecurring) "Yes (${expense.recurringFrequency})" else "No")).append(",")
            sb.append(escapeCsv(expense.note))
            sb.append("\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("#")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    /**
     * Shares or saves the CSV file using Android's native share sheet.
     */
    fun shareCsvFile(context: Context, csvData: String, householdName: String = "Household"): Boolean {
        val monthFileFormat = SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(Date())
        val cleanHousehold = householdName.replace("\\s+".toRegex(), "_")
        val filename = "Expenses_${cleanHousehold}_${monthFileFormat}.csv"

        return try {
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val file = File(cacheDir, filename)
            FileWriter(file).use { writer ->
                writer.write(csvData)
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "$householdName Expenses Export - $monthFileFormat")
                putExtra(Intent.EXTRA_TEXT, "Attached is the CSV export of household expenses for $monthFileFormat.")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Export Expenses CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "$householdName Expenses Export (CSV)")
                    putExtra(Intent.EXTRA_TEXT, csvData)
                }
                val chooser = Intent.createChooser(sendIntent, "Export Expenses CSV")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                true
            } catch (ex: Exception) {
                ex.printStackTrace()
                false
            }
        }
    }
}
