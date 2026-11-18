package com.quickgig.app.data.repository

import com.quickgig.app.data.model.Notification
import com.quickgig.app.data.model.PublicProfile
import com.quickgig.app.data.remote.QuickGigApi
import com.quickgig.app.data.remote.SessionManager
import com.quickgig.app.data.remote.UpdateSettingsRequest

class UserRepository(
    private val api: QuickGigApi,
    private val sessionManager: SessionManager,
) {
    suspend fun getProfile(userId: Long): Result<PublicProfile> =
        runCatching {
            val response = api.getProfile(userId)
            response.body()?.takeIf { response.isSuccessful }
                ?: error("Couldn't load profile (HTTP ${response.code()})")
        }

    suspend fun updateSettings(
        userId: Long,
        name: String? = null,
        phone: String? = null,
        preferredLanguage: String? = null,
        campusArea: String? = null,
        photoUrl: String? = null,
        notificationsEnabled: Boolean? = null,
    ): Result<Unit> = runCatching {
        // Apply the language on-device first, so the UI switches instantly and
        // still works with no connection; saving it on the server is best-effort.
        preferredLanguage?.let { sessionManager.setLanguage(it) }

        val response = api.updateSettings(userId, UpdateSettingsRequest(
            name = name, phone = phone, preferredLanguage = preferredLanguage,
            campusArea = campusArea, photoUrl = photoUrl, notificationsEnabled = notificationsEnabled,
        ))
        if (!response.isSuccessful) error("Couldn't save settings (HTTP ${response.code()})")
    }

    // ---- Notifications ----

    suspend fun fetchNotifications(after: Long? = null, limit: Int? = null): Result<Map<String, Any>> =
        runCatching {
            val response = api.listNotifications(after, limit)
            response.body()?.takeIf { response.isSuccessful }
                ?: error("Couldn't load notifications (HTTP ${response.code()})")
        }

    suspend fun markNotificationRead(ids: List<Long> = emptyList()): Result<Unit> =
        runCatching {
            val body = if (ids.isEmpty()) mapOf<String, Any>() else mapOf("ids" to ids)
            val response = api.markNotificationsRead(body)
            if (!response.isSuccessful) error("Couldn't mark read (HTTP ${response.code()})")
        }

    val currentUserId get() = sessionManager.userIdFlow
    val preferredLanguage get() = sessionManager.languageFlow
}

/** Converts the raw JSON notification map (from the API) into a Notification model. */
fun Map<*, *>.toNotification(): Notification =
    Notification(
        id = (this["id"] as? Number)?.toLong() ?: 0,
        type = this["type"]?.toString() ?: "",
        title = this["title"]?.toString() ?: "",
        body = this["body"]?.toString() ?: "",
        gigId = (this["gig_id"] as? Number)?.toLong(),
        createdAt = this["created_at"]?.toString() ?: "",
        read = this["read"] as? Boolean ?: false,
    )
