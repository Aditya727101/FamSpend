package com.example.util

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WeeklyReminderScheduler {

    private const val WORK_NAME_WEEKLY = "weekly_budget_reminder_work"
    private const val PREFS_NAME = "weekly_reminder_prefs"
    private const val KEY_REMINDER_ENABLED = "is_weekly_reminder_enabled"

    fun isReminderEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_REMINDER_ENABLED, true)
    }

    fun setReminderEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()

        if (enabled) {
            scheduleWeeklyReminder(context)
        } else {
            cancelWeeklyReminder(context)
        }
    }

    fun scheduleWeeklyReminder(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)

            // Calculate delay until next Sunday at 6:00 PM
            val calendar = Calendar.getInstance()
            val nowMillis = calendar.timeInMillis

            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 18) // 6 PM
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)

            if (calendar.timeInMillis <= nowMillis) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }

            val initialDelayMillis = calendar.timeInMillis - nowMillis

            val weeklyWorkRequest = PeriodicWorkRequestBuilder<WeeklyBudgetReminderWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME_WEEKLY,
                ExistingPeriodicWorkPolicy.UPDATE,
                weeklyWorkRequest
            )

            Log.d("WeeklyReminderScheduler", "Weekly reminder scheduled to run every Sunday at 6 PM. Initial delay: ${initialDelayMillis / 1000 / 60} minutes")
        } catch (e: Exception) {
            Log.e("WeeklyReminderScheduler", "Error scheduling weekly reminder work", e)
        }
    }

    fun cancelWeeklyReminder(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_WEEKLY)
            Log.d("WeeklyReminderScheduler", "Weekly reminder work cancelled.")
        } catch (e: Exception) {
            Log.e("WeeklyReminderScheduler", "Error cancelling weekly reminder work", e)
        }
    }

    fun triggerImmediateTestReminder(context: Context) {
        try {
            val testRequest = OneTimeWorkRequestBuilder<WeeklyBudgetReminderWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "test_weekly_reminder_work",
                ExistingWorkPolicy.REPLACE,
                testRequest
            )
            Log.d("WeeklyReminderScheduler", "Immediate test reminder enqueued.")
        } catch (e: Exception) {
            Log.e("WeeklyReminderScheduler", "Error triggering test reminder", e)
        }
    }
}
