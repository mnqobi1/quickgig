package com.quickgig.app.data.remote

import com.quickgig.app.data.model.GeoPoint
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val preferredLanguage: String? = null,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class GoogleSsoRequest(val idToken: String)

@Serializable
data class CreateGigRequest(
    val title: String,
    val category: String,
    val description: String? = null,
    val priceCredits: Int = 0,
    val location: GeoPoint? = null,
    val neededBy: String? = null,
)

@Serializable
data class UpdateStatusRequest(
    val status: String,
    val assignedTo: Long? = null,
    val stars: Int? = null,
    val onTime: Boolean? = null,
)

@Serializable
data class UpdateSettingsRequest(
    val name: String? = null,
    val phone: String? = null,
    val preferredLanguage: String? = null,
    val campusArea: String? = null,
    val photoUrl: String? = null,
    val notificationsEnabled: Boolean? = null,
)

@Serializable
data class SendMessageRequest(val recipientId: Long, val body: String)
