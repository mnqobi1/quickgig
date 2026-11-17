package com.quickgig.app.sync

/**
 * User Defined Feature 2: offline mode with sync.
 *
 * Pure conflict-resolution logic for merging a locally-cached record with
 * whatever the server returns once connectivity is back, per the Planning
 * doc: "a WorkManager job detects reconnection and pushes queued drafts
 * and messages, resolving conflicts by keeping the most recent server
 * timestamp." Kept free of Room/WorkManager types so it can be unit
 * tested directly; SyncWorker is the thin Android wrapper that calls this
 * against real DAOs and the API.
 */

enum class SyncState { SYNCED, PENDING_CREATE, PENDING_UPDATE, CONFLICT }

data class SyncableRecord(
    val localId: String,
    val remoteId: String?,
    val updatedAtEpochMs: Long,
    val state: SyncState,
)

sealed class SyncResolution {
    data class PushLocal(val record: SyncableRecord) : SyncResolution()
    data class AdoptRemote(val record: SyncableRecord) : SyncResolution()
    object NoOp : SyncResolution()
}

object SyncMerge {

    fun resolve(local: SyncableRecord, remote: SyncableRecord?): SyncResolution {
        if (local.state == SyncState.SYNCED && remote == null) {
            return SyncResolution.NoOp
        }
        if (remote == null) {
            return SyncResolution.PushLocal(local)
        }
        if (local.state == SyncState.SYNCED) {
            return if (remote.updatedAtEpochMs > local.updatedAtEpochMs) {
                SyncResolution.AdoptRemote(remote)
            } else {
                SyncResolution.NoOp
            }
        }
        return if (local.updatedAtEpochMs >= remote.updatedAtEpochMs) {
            SyncResolution.PushLocal(local)
        } else {
            SyncResolution.AdoptRemote(remote)
        }
    }

    fun resolveAll(
        localRecords: List<SyncableRecord>,
        remoteById: Map<String, SyncableRecord>,
    ): List<SyncResolution> = localRecords.map { local ->
        resolve(local, local.remoteId?.let { remoteById[it] })
    }
}
