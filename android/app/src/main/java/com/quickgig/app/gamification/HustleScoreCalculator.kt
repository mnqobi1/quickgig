package com.quickgig.app.gamification

/**
 * User Defined Feature 1: the "Hustle Score" gamification system.
 *
 * Mirrors the scoring rule implemented server-side in `api/src/hustle.js`
 * exactly, so the app can show a confident preview of points-to-be-earned
 * before the server confirms it. The server remains the source of truth —
 * points are only actually credited via the API's PATCH /gigs/:id/status
 * response (see GigRepository).
 *
 * Deliberately has zero Android framework dependencies so it can be unit
 * tested with plain JUnit, no emulator or Robolectric required.
 */
object HustleScoreCalculator {

    private const val BASE_POINTS = 10
    private const val MAX_RATING_BONUS = 10
    private const val ON_TIME_BONUS = 5

    /** Points awarded for completing a gig, given the requester's rating and punctuality. */
    fun pointsForCompletion(stars: Double, onTime: Boolean): Int {
        val clamped = stars.coerceIn(0.0, 5.0)
        val ratingPart = Math.round((clamped / 5.0) * MAX_RATING_BONUS).toInt()
        val onTimePart = if (onTime) ON_TIME_BONUS else 0
        return BASE_POINTS + ratingPart + onTimePart
    }

    data class BadgeProgress(
        val code: String,
        val label: String,
        val unlocked: Boolean,
        val progress: Float,
    )

    data class UserStats(
        val verified: Boolean,
        val completedCount: Int,
        val lateCancellations: Int,
        val ratingCount: Int,
        val averageRating: Double,
    )

    fun evaluateBadges(stats: UserStats): List<BadgeProgress> = listOf(
        BadgeProgress(
            code = "verified_student",
            label = "Verified Student",
            unlocked = stats.verified,
            progress = if (stats.verified) 1f else 0f,
        ),
        BadgeProgress(
            code = "top_rated",
            label = "Top Rated",
            unlocked = stats.ratingCount >= 5 && stats.averageRating >= 4.5,
            progress = (stats.ratingCount / 5f).coerceIn(0f, 1f),
        ),
        BadgeProgress(
            code = "reliable_runner",
            label = "Reliable Runner",
            unlocked = stats.completedCount >= 10 && stats.lateCancellations == 0,
            progress = if (stats.lateCancellations > 0) 0f else (stats.completedCount / 10f).coerceIn(0f, 1f),
        ),
    )
}
