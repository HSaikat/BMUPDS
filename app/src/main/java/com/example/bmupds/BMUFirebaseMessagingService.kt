package com.example.bmupds

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

object BmuPushNotifications {
    const val TOPIC_ALL_USERS = "all_users"
    const val EXTRA_DEEP_URL = "deep_url"
    const val CHANNEL_ID = "bmu_updates"

    fun initialize(context: Context) {
        createChannel(context)
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_ALL_USERS)
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "BMU Portal Updates"
            val descriptionText = "Notifications for BMU PDS announcements and updates"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun safeDeepUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(rawUrl)
            if (uri.scheme == "https" &&
                (uri.host == "pds.bmu.ac.bd" || uri.host == "attendance.bmu.ac.bd") &&
                (uri.port == -1 || uri.port == 443) &&
                uri.userInfo.isNullOrEmpty()
            ) {
                uri.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

class BMUFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "BMU PDS"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "New update available"
        val rawDeepUrl = remoteMessage.notification?.let { null }
            ?: remoteMessage.data[BmuPushNotifications.EXTRA_DEEP_URL]

        val validatedDeepUrl = BmuPushNotifications.safeDeepUrl(rawDeepUrl)

        showNotification(title, body, validatedDeepUrl)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseMessaging.getInstance().subscribeToTopic(BmuPushNotifications.TOPIC_ALL_USERS)
    }

    private fun showNotification(title: String, body: String, deepUrl: String?) {
        val context = applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!deepUrl.isNullOrEmpty()) {
                putExtra(BmuPushNotifications.EXTRA_DEEP_URL, deepUrl)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, BmuPushNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bmu_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
