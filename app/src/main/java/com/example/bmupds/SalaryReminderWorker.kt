package com.example.bmupds

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SalaryReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        const val CHANNEL_ID      = "bmu_salary_reminder"
        const val NOTIF_ID        = 1001
        const val WORK_TAG        = "salary_reminder_work"
        const val PREFS_NAME      = "bmu_salary_prefs"
        const val KEY_SUBMITTED   = "submitted_"   // + "YYYY_MM"
        const val KEY_MUTED       = "muted_"       // + "YYYY_MM"
        const val INTERVAL_DAYS   = 3L

        fun yearMonth(): String {
            val c = Calendar.getInstance()
            return "${c.get(Calendar.YEAR)}_${(c.get(Calendar.MONTH)+1).toString().padStart(2,'0')}"
        }

        fun schedule(context: Context) {
            createChannel(context)
            val req = PeriodicWorkRequestBuilder<SalaryReminderWorker>(
                INTERVAL_DAYS, TimeUnit.DAYS
            )
                .addTag(WORK_TAG)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }

        fun markSubmitted(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean("$KEY_SUBMITTED${yearMonth()}", true).apply()
            cancel(context)
        }

        fun markMuted(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean("$KEY_MUTED${yearMonth()}", true).apply()
            cancel(context)
        }

        fun isDone(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val ym = yearMonth()
            return prefs.getBoolean("$KEY_SUBMITTED$ym", false) ||
                   prefs.getBoolean("$KEY_MUTED$ym", false)
        }

        private fun cancel(context: Context) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(NOTIF_ID)
        }

        private fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "Salary Bill Reminder",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminds you to submit your monthly salary bill."
                    enableVibration(true)
                }
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(ch)
            }
        }
    }

    override fun doWork(): Result {
        if (isDone(context)) return Result.success()

        val cal   = Calendar.getInstance()
        // Only notify from day 1 of the month onwards
        if (cal.get(Calendar.DAY_OF_MONTH) < 1) return Result.success()

        val monthName = android.text.format.DateFormat.format("MMMM", cal.time).toString()

        // Deep-link intent — opens the salary list page directly
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("deep_url",
                "https://pds.bmu.ac.bd/pds/user_mod/pages/salary_ration_money/salary_money_monthly_list.php")
        }
        val openPi = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mute action — fires MuteReminderReceiver
        val muteIntent = Intent(context, MuteReminderReceiver::class.java)
        val mutePi = PendingIntent.getBroadcast(
            context, 1, muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("বেতন বিল জমা দিন 💰")
            .setContentText("$monthName মাসের বেতন বিল এখনো জমা দেওয়া হয়নি।")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$monthName মাসের বেতন বিল এখনো জমা দেওয়া হয়নি। দেরি না করে এখনই জমা দিন।")
            )
            .setContentIntent(openPi)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "এই মাসের জন্য বন্ধ করুন",
                mutePi
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID, notif)

        return Result.success()
    }
}
