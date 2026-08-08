package com.example.util

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import java.util.Locale

class WeeklyBudgetReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(context)
            val defaultHouseholdId = "FAM-7892-OAK"
            val household = db.householdDao().getHouseholdById(defaultHouseholdId).firstOrNull()
            
            val totalBudget = household?.totalMonthlyBudget ?: 3200.0
            val currencySymbol = household?.defaultCurrency ?: "$"

            val expenses = db.expenseDao().getExpensesByHousehold(defaultHouseholdId).firstOrNull() ?: emptyList()

            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)

            val monthExpenses = expenses.filter { exp ->
                val expCal = Calendar.getInstance().apply { timeInMillis = exp.timestamp }
                expCal.get(Calendar.MONTH) == currentMonth && expCal.get(Calendar.YEAR) == currentYear
            }

            val totalSpent = monthExpenses.sumOf { it.amount }
            val percentUsed = if (totalBudget > 0) ((totalSpent / totalBudget) * 100).toInt() else 0

            val notifTitle = "🗓️ End-of-Week Budget & Expense Review"
            val notifMessage = if (totalBudget > 0) {
                "You've spent $currencySymbol${String.format(Locale.US, "%.2f", totalSpent)} this month ($percentUsed% of your $currencySymbol${totalBudget.toInt()} budget). Tap to check savings insights & log recent expenses!"
            } else {
                "You've logged ${monthExpenses.size} expenses totaling $currencySymbol${String.format(Locale.US, "%.2f", totalSpent)} this month. Tap to review your family budget status!"
            }

            sendNotification(context, notifTitle, notifMessage)
            Result.success()
        } catch (e: Exception) {
            Log.e("WeeklyReminderWorker", "Error executing weekly reminder worker", e)
            Result.retry()
        }
    }

    private fun sendNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            ?: return

        val channelId = "famspend_weekly_reminders"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Weekly Budget Reminders",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled end-of-week notifications to review monthly expenses and budget health"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(78901, builder.build())
    }
}
