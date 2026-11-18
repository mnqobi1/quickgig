package com.quickgig.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.quickgig.app.data.remote.RetrofitClient
import com.quickgig.app.data.remote.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Firebase Cloud Messaging entry point.
 *
 * 1. When the app gets a fresh FCM registration token, send it to
 *    POST /api/devices so the server can push notifications to this device.
 * 2. When a data-only FCM payload arrives (app closed or backgrounded), show a
 *    local notification — the server only ever sends title/body, so this
 *    renders exactly what was pushed.
 */
class QuickGigMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Register the token with the API so the server knows where to push.
        // Fire and forget — a failed registration must never crash the app.
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val session = SessionManager(this@QuickGigMessagingService)
                    val api = RetrofitClient.create(session)
                    api.registerDevice(mapOf("token" to token))
                } catch (e: Exception) {
                    // ignored — token will be retried on next app launch
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        if (data.isEmpty()) return

        val title = data["title"] ?: resources.getString(com.quickgig.app.R.string.fcm_default_title)
        val body = data["body"] ?: resources.getString(com.quickgig.app.R.string.fcm_default_body)
        val gigId = data["gigId"].takeIf { it.isNotEmpty() }?.toLongOrNull()

        showNotification(title, body, gigId)
    }

    private fun showNotification(title: String, body: String, gigId: Long?) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    "quickgig_push",
                    resources.getString(com.quickgig.app.R.string.fcm_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = resources.getString(com.quickgig.app.R.string.fcm_channel_description)
                }
            )
        }

        val intent = android.content.Intent(this, MainActivity::class.java).apply {
            gigId?.let { putExtra("gigId", it) }
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pending = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, "quickgig_push")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)

        manager.notify(gigId?.hashCode() ?: 0, builder.build())
    }
}
