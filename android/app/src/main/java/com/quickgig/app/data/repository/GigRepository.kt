package com.quickgig.app.data.repository

import com.quickgig.app.data.local.CachedGigEntity
import com.quickgig.app.data.local.DraftGigEntity
import com.quickgig.app.data.local.GigDao
import com.quickgig.app.data.local.DraftGigDao
import com.quickgig.app.data.model.GeoPoint
import com.quickgig.app.data.model.Gig
import com.quickgig.app.data.remote.CreateGigRequest
import com.quickgig.app.data.remote.NetworkMonitor
import com.quickgig.app.data.remote.QuickGigApi
import com.quickgig.app.data.remote.UpdateStatusRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class PostGigResult {
    data class PostedLive(val gig: Gig) : PostGigResult()
    object SavedOffline : PostGigResult()
    data class Error(val message: String) : PostGigResult()
}

/**
 * User Defined Feature 2 (offline mode with sync) lives mostly here. Reads
 * come from the Room cache (so the feed and "My Gigs" render instantly and
 * work offline); a network refresh updates that cache when available.
 * Writes made offline become DraftGigEntity rows with pendingSync = true
 * and are pushed by SyncWorker once connectivity returns.
 */
class GigRepository(
    private val api: QuickGigApi,
    private val gigDao: GigDao,
    private val draftGigDao: DraftGigDao,
    private val networkMonitor: NetworkMonitor,
) {
    fun observeFeed(): Flow<List<CachedGigEntity>> = gigDao.observeAll()
    fun observeMyGigs(userId: Long): Flow<List<CachedGigEntity>> = gigDao.observeMine(userId)
    fun observePendingDrafts(): Flow<List<DraftGigEntity>> = draftGigDao.observePending()

    suspend fun refreshFeed(lat: Double?, lng: Double?, radiusKm: Double?, category: String?) {
        if (!networkMonitor.isOnline()) return
        runCatching { api.listGigs(lat, lng, radiusKm, category?.takeIf { it != "all" }) }
            .onSuccess { response ->
                val gigs = response.body()?.gigs ?: return@onSuccess
                gigDao.upsertAll(gigs.map { it.toCachedEntity() })
            }
    }

    suspend fun postGig(
        title: String,
        category: String,
        description: String?,
        priceCredits: Int,
        location: GeoPoint?,
        neededBy: String?,
    ): PostGigResult {
        if (!networkMonitor.isOnline()) {
            draftGigDao.insert(
                DraftGigEntity(
                    title = title, description = description, category = category,
                    priceCredits = priceCredits, lat = location?.lat, lng = location?.lng,
                    neededBy = neededBy, createdAtEpochMs = System.currentTimeMillis(),
                )
            )
            return PostGigResult.SavedOffline
        }

        val request = CreateGigRequest(title, category, description, priceCredits, location, neededBy)
        return runCatching { api.createGig(request) }.fold(
            onSuccess = { response ->
                val gig = response.body()
                if (response.isSuccessful && gig != null) {
                    gigDao.upsertAll(listOf(gig.toCachedEntity()))
                    PostGigResult.PostedLive(gig)
                } else {
                    draftGigDao.insert(
                        DraftGigEntity(
                            title = title, description = description, category = category,
                            priceCredits = priceCredits, lat = location?.lat, lng = location?.lng,
                            neededBy = neededBy, createdAtEpochMs = System.currentTimeMillis(),
                            syncError = "HTTP ${response.code()}",
                        )
                    )
                    PostGigResult.SavedOffline
                }
            },
            onFailure = {
                draftGigDao.insert(
                    DraftGigEntity(
                        title = title, description = description, category = category,
                        priceCredits = priceCredits, lat = location?.lat, lng = location?.lng,
                        neededBy = neededBy, createdAtEpochMs = System.currentTimeMillis(),
                    )
                )
                PostGigResult.SavedOffline
            },
        )
    }

    suspend fun trySyncDraft(draft: DraftGigEntity): Boolean {
        val request = CreateGigRequest(
            title = draft.title, category = draft.category, description = draft.description,
            priceCredits = draft.priceCredits, location = GeoPoint(draft.lat, draft.lng), neededBy = draft.neededBy,
        )
        return runCatching { api.createGig(request) }
            .map { response ->
                val gig = response.body()
                if (response.isSuccessful && gig != null) {
                    gigDao.upsertAll(listOf(gig.toCachedEntity()))
                    draftGigDao.delete(draft)
                    true
                } else {
                    false
                }
            }
            .getOrDefault(false)
    }

    suspend fun applyToGig(gigId: Long): Result<Unit> =
        runCatching { api.applyToGig(gigId) }.map { }

    suspend fun updateStatus(gigId: Long, status: String, assignedTo: Long?, stars: Int?, onTime: Boolean?): Result<Unit> =
        runCatching { api.updateGigStatus(gigId, UpdateStatusRequest(status, assignedTo, stars, onTime)) }.map { }
}

private fun Gig.toCachedEntity() = CachedGigEntity(
    remoteId = id, title = title, description = description, category = category,
    priceCredits = priceCredits, lat = location.lat, lng = location.lng, neededBy = neededBy,
    status = status, postedBy = postedBy, assignedTo = assignedTo, createdAt = createdAt,
    updatedAtEpochMs = System.currentTimeMillis(),
)

fun Flow<List<CachedGigEntity>>.toUiGigs(): Flow<List<Gig>> = map { list ->
    list.map {
        Gig(
            id = it.remoteId, title = it.title, description = it.description, category = it.category,
            priceCredits = it.priceCredits, location = GeoPoint(it.lat, it.lng), neededBy = it.neededBy,
            status = it.status, postedBy = it.postedBy, assignedTo = it.assignedTo, createdAt = it.createdAt,
        )
    }
}
