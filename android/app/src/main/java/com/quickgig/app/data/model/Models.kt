package com.quickgig.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeoPoint(val lat: Double? = null, val lng: Double? = null)

@Serializable
data class User(
    val id: Long,
    val name: String,
    val email: String? = null,
    val preferredLanguage: String = "en",
    val campusArea: String? = null,
    val hustleScore: Int = 0,
    val verificationStatus: String = "unverified",
)

@Serializable
data class Badge(
    @SerialName("badge_code") val badgeCode: String,
    @SerialName("unlocked_at") val unlockedAt: String,
)

@Serializable
data class PublicProfile(
    val id: Long,
    val name: String,
    val photoUrl: String? = null,
    val campusArea: String? = null,
    val hustleScore: Int = 0,
    val verificationStatus: String = "unverified",
    val badges: List<Badge> = emptyList(),
    val completedGigs: Int = 0,
    val averageRating: Double = 0.0,
)

@Serializable
data class AuthResponse(val token: String, val user: User)

@Serializable
data class Gig(
    val id: Long,
    val title: String,
    val description: String? = null,
    val category: String,
    val priceCredits: Int = 0,
    val location: GeoPoint = GeoPoint(),
    val neededBy: String? = null,
    val status: String = "open",
    val postedBy: Long,
    val assignedTo: Long? = null,
    val createdAt: String,
    val distanceKm: Double? = null,
)

@Serializable
data class GigListResponse(val gigs: List<Gig>)

@Serializable
data class ChatMessage(
    val id: Long,
    @SerialName("gig_id") val gigId: Long,
    @SerialName("sender_id") val senderId: Long,
    @SerialName("recipient_id") val recipientId: Long,
    val body: String,
    @SerialName("created_at") val createdAt: String,
)

// Display labels live in strings.xml (see categoryLabelRes() in the UI layer)
// so they translate with the rest of the multi-language UI.
enum class GigCategory(val apiValue: String) {
    ALL("all"),
    ERRANDS("errands"),
    TUTORING("tutoring"),
    CREATIVE("creative"),
    LABOUR("labour"),
}

enum class SupportedLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    ISIZULU("zu", "isiZulu"),
    AFRIKAANS("af", "Afrikaans"),
}
