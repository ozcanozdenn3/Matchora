package com.matchora.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.matchora.app.MainActivity

fun initializePush() {
    if (BackendBuildConfig.MODE == "preview" || BackendBuildConfig.FIREBASE_APP_ID.isBlank()) return
    val context = AndroidBackendRuntime.context
    runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
            .setApplicationId(BackendBuildConfig.FIREBASE_APP_ID).setApiKey(BackendBuildConfig.FIREBASE_API_KEY)
            .setProjectId(BackendBuildConfig.FIREBASE_PROJECT_ID).setGcmSenderId(BackendBuildConfig.FIREBASE_SENDER_ID).build())
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        FirebaseMessaging.getInstance().token.addOnSuccessListener { DeviceEventBridge.updatePushToken("android", it) }
    }
}

class MatchoraMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) { DeviceEventBridge.updatePushToken("android", token) }
    override fun onMessageReceived(message: RemoteMessage) {
        if (BackendBuildConfig.MODE == "preview") return
        val destination = message.data["destination"].orEmpty()
        if (destination !in listOf(
                "EVENT", "CHAT", "CHALLENGES", "TEAM", "TOURNAMENT", "WALLET", "PLUS", "ACCOUNT",
                "RANKING", "PASSPORT", "PLAY", "NETWORK", "VENUES", "MEMORIES", "MODERATION",
                "RESERVATIONS", "VENUE_MANAGEMENT"
            )) return
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("matchora_activity", "Maçlar ve mesajlar", NotificationManager.IMPORTANCE_DEFAULT))
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        val intent = Intent(this, MainActivity::class.java)
            .putExtra("destination", destination)
            .putExtra("targetId", message.data["targetId"].orEmpty())
            .putExtra("notificationId", message.data["notificationId"].orEmpty())
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val id = (message.messageId ?: message.data["notificationId"] ?: message.sentTime.toString()).hashCode()
        val pending = PendingIntent.getActivity(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, "matchora_activity")
            .setSmallIcon(com.matchora.app.R.drawable.ic_notification)
            .setContentTitle(message.notification?.title ?: message.data["title"] ?: "Matchora")
            .setContentText(message.notification?.body ?: message.data["body"] ?: "Yeni bir güncelleme var.")
            .setAutoCancel(true).setContentIntent(pending).build()
        try { manager.notify(id, notification) } catch (_: SecurityException) { }
    }
}
