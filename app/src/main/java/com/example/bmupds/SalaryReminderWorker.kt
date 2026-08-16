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
        const val CHANNEL_ID    = "bmu_salary_reminder"
        const val NOTIF_ID      = 1001
        const val WORK_TAG      = "salary_reminder"
        const val PREFS_NAME    = "bmu_salary_prefs"
        const val INTERVAL_DAYS = 3L

        /* Returns "YYYY_MM" for current month */
        fun ym(): String {
            val c = Calendar.getInstance()
            return "${c.get(Calendar.YEAR)}_${(c.get(Calendar.MONTH)+1).toString().padStart(2,'0')}"
        }

        /* Called by MainActivity when submission redirect is detected */
        fun markSubmitted(context: Context) {
            prefs(context).edit().putBoolean("submitted_${ym()}", true).apply()
            cancel(context)
        }

        /* Called when user taps "Mute this month" on the notification */
        fun markMuted(context: Context) {
            prefs(context).edit().putBoolean("muted_${ym()}", true).apply()
            cancel(context)
        }

        fun isDone(context: Context): Boolean {
            val p = prefs(context); val ym = ym()
            return p.getBoolean("submitted_$ym", false) || p.getBoolean("muted_$ym", false)
        }

        fun schedule(context: Context) {
            createChannel(context)
            /* Fire on day 1 of each month, then every 3 days */
            val now = Calendar.getInstance()
            val firstOfMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            /* If we're past day 1 already, use now as start */
            val initialDelay = if (now.after(firstOfMonth))
                0L
            else
                (firstOfMonth.timeInMillis - now.timeInMillis)

            val req = PeriodicWorkRequestBuilder<SalaryReminderWorker>(
                INTERVAL_DAYS, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }

        private fun cancel(ctx: Context) {
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(NOTIF_ID)
        }

        private fun prefs(ctx: Context) =
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        private fun createChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ch = NotificationChannel(
                    CHANNEL_ID, "Salary Bill Reminder",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminds you to submit your monthly salary bill."
                    enableVibration(true)
                }
                (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(ch)
            }
        }
    }

    override fun doWork(): Result {
        if (isDone(context)) return Result.success()

        val monthName = java.text.SimpleDateFormat("MMMM", java.util.Locale.ENGLISH)
            .format(Calendar.getInstance().time)

        /* Tap notification → open the list page directly */
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("deep_url",
                "https://pds.bmu.ac.bd/pds/user_mod/pages/salary_ration_money/salary_money_monthly_list.php")
        }
        val openPi = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /* Mute action → fires MuteReminderReceiver */
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
                    .bigText("$monthName মাসের বেতন বিল এখনো জমা দেওয়া হয়নি।\nএখনই জমা দিন।")
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
