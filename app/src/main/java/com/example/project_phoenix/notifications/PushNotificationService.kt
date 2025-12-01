package com.example.project_phoenix.notifications

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.project_phoenix.R
import com.example.project_phoenix.data.SettingsRepository
import com.example.project_phoenix.ui.app.MainActivity
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val repository = SettingsRepository(applicationContext)
        if (repository.isNotificationsEnabled()) {
            FirebaseMessaging.getInstance().subscribeToTopic(NotificationConstants.GENERAL_TOPIC)
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(NotificationConstants.GENERAL_TOPIC)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val repository = SettingsRepository(applicationContext)
        if (!repository.isNotificationsEnabled()) return

        NotificationHelper.ensureNotificationChannel(this)

        val notificationTitle = remoteMessage.notification?.title
            ?: getString(R.string.push_notification_title)
        val notificationBody = remoteMessage.notification?.body
            ?: getString(R.string.push_notification_body)

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notificationTitle)
            .setContentText(notificationBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = remoteMessage.messageId?.hashCode()
            ?: System.currentTimeMillis().toInt()

        NotificationManagerCompat.from(this)
            .notify(notificationId, builder.build())
    }

    companion object {
        private const val TAG = "PushNotificationSvc"
    }
}