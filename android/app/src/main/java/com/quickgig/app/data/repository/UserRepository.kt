package com.quickgig.app.data.repository

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
    ): Result<Unit> = runCatching {
        // Apply the language on-device first, so the UI switches instantly and
        // still works with no connection; saving it on the server is best-effort.
        preferredLanguage?.let { sessionManager.setLanguage(it) }

        val response = api.updateSettings(userId, UpdateSettingsRequest(name, phone, preferredLanguage, campusArea))
        if (!response.isSuccessful) error("Couldn't save settings (HTTP ${response.code()})")
    }

    val currentUserId get() = sessionManager.userIdFlow
    val preferredLanguage get() = sessionManager.languageFlow
}
