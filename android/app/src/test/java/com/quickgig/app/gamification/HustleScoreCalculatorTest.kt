package com.quickgig.app.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HustleScoreCalculatorTest {

    @Test
    fun `base points only for a zero-star late completion`() {
        assertEquals(10, HustleScoreCalculator.pointsForCompletion(stars = 0.0, onTime = false))
    }

    @Test
    fun `full points for a five-star on-time completion`() {
        assertEquals(25, HustleScoreCalculator.pointsForCompletion(stars = 5.0, onTime = true))
    }

    @Test
    fun `rating bonus scales proportionally`() {
        assertEquals(15, HustleScoreCalculator.pointsForCompletion(stars = 2.5, onTime = false))
    }

    @Test
    fun `out-of-range stars are clamped rather than throwing`() {
        assertEquals(20, HustleScoreCalculator.pointsForCompletion(stars = 8.0, onTime = false))
        assertEquals(10, HustleScoreCalculator.pointsForCompletion(stars = -3.0, onTime = false))
    }

    @Test
    fun `verified student badge unlocks as soon as verification is true`() {
        val stats = HustleScoreCalculator.UserStats(
            verified = true, completedCount = 0, lateCancellations = 0, ratingCount = 0, averageRating = 0.0,
        )
        val badge = HustleScoreCalculator.evaluateBadges(stats).first { it.code == "verified_student" }
        assertTrue(badge.unlocked)
    }

    @Test
    fun `top rated badge needs at least five ratings averaging 4point5`() {
        val notEnoughRatings = HustleScoreCalculator.UserStats(
            verified = false, completedCount = 5, lateCancellations = 0, ratingCount = 4, averageRating = 5.0,
        )
        val lowAverage = HustleScoreCalculator.UserStats(
            verified = false, completedCount = 5, lateCancellations = 0, ratingCount = 5, averageRating = 4.0,
        )
        val qualifies = HustleScoreCalculator.UserStats(
            verified = false, completedCount = 5, lateCancellations = 0, ratingCount = 5, averageRating = 4.6,
        )
        assertFalse(HustleScoreCalculator.evaluateBadges(notEnoughRatings).first { it.code == "top_rated" }.unlocked)
        assertFalse(HustleScoreCalculator.evaluateBadges(lowAverage).first { it.code == "top_rated" }.unlocked)
        assertTrue(HustleScoreCalculator.evaluateBadges(qualifies).first { it.code == "top_rated" }.unlocked)
    }

    @Test
    fun `reliable runner needs ten completions and zero late cancellations`() {
        val oneCancellation = HustleScoreCalculator.UserStats(
            verified = false, completedCount = 12, lateCancellations = 1, ratingCount = 0, averageRating = 0.0,
        )
        val qualifies = HustleScoreCalculator.UserStats(
            verified = false, completedCount = 10, lateCancellations = 0, ratingCount = 0, averageRating = 0.0,
        )
        assertFalse(HustleScoreCalculator.evaluateBadges(oneCancellation).first { it.code == "reliable_runner" }.unlocked)
        assertTrue(HustleScoreCalculator.evaluateBadges(qualifies).first { it.code == "reliable_runner" }.unlocked)
    }
}
