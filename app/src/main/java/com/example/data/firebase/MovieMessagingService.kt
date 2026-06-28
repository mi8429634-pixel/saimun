package com.example.data.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MovieMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
        saveTokenToCloud(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        // Extract title, body and category
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "New Update"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Check out what's new in Movie Premium!"
        val category = remoteMessage.data["category"] ?: "announcements" // movie_releases, watchlist, announcements

        sendSystemNotification(title, body, category)
    }

    private fun sendSystemNotification(title: String, body: String, category: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = getChannelIdForCategory(category)

        // Ensure notification channel is created for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getChannelNameForCategory(category)
            val channelDescription = getChannelDescriptionForCategory(category)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap action intent targeting MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("fcm_category", category)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Try using modern material application icon if present (fallback to system app icon)
        val iconRes = applicationContext.applicationInfo.icon

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MovieMessaging"

        // CHANNEL IDs
        const val CHANNEL_RELEASES = "movie_releases"
        const val CHANNEL_WATCHLIST = "watchlist_updates"
        const val CHANNEL_ANNOUNCEMENTS = "announcements"

        fun getChannelIdForCategory(category: String): String {
            return when (category) {
                "movie_releases" -> CHANNEL_RELEASES
                "watchlist" -> CHANNEL_WATCHLIST
                else -> CHANNEL_ANNOUNCEMENTS
            }
        }

        fun getChannelNameForCategory(category: String): String {
            return when (category) {
                "movie_releases" -> "New Movie Releases"
                "watchlist" -> "Watchlist & Favorites"
                else -> "Administrative announcements"
            }
        }

        fun getChannelDescriptionForCategory(category: String): String {
            return when (category) {
                "movie_releases" -> "Get notified instantly when new movies, seasons, or blockbusters are uploaded."
                "watchlist" -> "Stay updated on modifications, content adjustments, or reminders for bookmarked movies."
                else -> "General notifications, maintenance updates, and streaming alerts from administrators."
            }
        }

        // Setup notification channels on app startup
        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
                val channels = listOf(
                    NotificationChannel(CHANNEL_RELEASES, "New Movie Releases", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "Get notified instantly when new movies, seasons, or blockbusters are uploaded."
                    },
                    NotificationChannel(CHANNEL_WATCHLIST, "Watchlist & Favorites", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "Stay updated on modifications, content adjustments, or reminders for bookmarked movies."
                    },
                    NotificationChannel(CHANNEL_ANNOUNCEMENTS, "Administrative announcements", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "General notifications, maintenance updates, and streaming alerts from administrators."
                    }
                )
                
                for (channel in channels) {
                    notificationManager.createNotificationChannel(channel)
                }
            }
        }

        // Subscribe / Unsubscribe helper engines
        fun subscribeToTopic(topic: String, onResult: (Boolean) -> Unit = {}) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Successfully subscribed to topic: $topic")
                        onResult(true)
                    } else {
                        Log.e(TAG, "Subscription to topic: $topic failed: ${task.exception?.message}")
                        onResult(false)
                    }
                }
        }

        fun unsubscribeFromTopic(topic: String, onResult: (Boolean) -> Unit = {}) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Successfully unsubscribed from topic: $topic")
                        onResult(true)
                    } else {
                        Log.e(TAG, "Unsubscription from topic: $topic failed: ${task.exception?.message}")
                        onResult(false)
                    }
                }
        }

        // Save Token to Firestore
        fun saveTokenToCloud(token: String) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val tokenData = mapOf(
                        "fcmToken" to token,
                        "lastTokenUpdate" to System.currentTimeMillis()
                    )
                    firestore.collection("users").document(currentUserId)
                        .set(tokenData, SetOptions.merge())
                        .await()
                    Log.d(TAG, "FCM Token mapped successfully in user firestore document.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to map FCM Token in Firestore: ${e.localizedMessage}")
                }
            }
        }

        // Manual trigger/sync token if already generated
        fun syncDeviceToken() {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    saveTokenToCloud(task.result)
                }
            }
        }
    }
}
