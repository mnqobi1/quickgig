package com.quickgig.app.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergeTest {

    private fun record(id: String, remoteId: String?, updatedAt: Long, state: SyncState) =
        SyncableRecord(localId = id, remoteId = remoteId, updatedAtEpochMs = updatedAt, state = state)

    @Test
    fun `a draft that never reached the server is always pushed`() {
        val draft = record("local-1", remoteId = null, updatedAt = 1000, state = SyncState.PENDING_CREATE)
        assertTrue(SyncMerge.resolve(draft, remote = null) is SyncResolution.PushLocal)
    }

    @Test
    fun `a synced record with no local edits adopts a newer remote copy`() {
        val local = record("local-2", remoteId = "g_2", updatedAt = 1000, state = SyncState.SYNCED)
        val remote = record("g_2", remoteId = "g_2", updatedAt = 2000, state = SyncState.SYNCED)
        val result = SyncMerge.resolve(local, remote)
        assertTrue(result is SyncResolution.AdoptRemote)
        assertEquals(2000, (result as SyncResolution.AdoptRemote).record.updatedAtEpochMs)
    }

    @Test
    fun `a synced record with no local edits and no newer remote change is a no-op`() {
        val local = record("local-3", remoteId = "g_3", updatedAt = 1000, state = SyncState.SYNCED)
        val remote = record("g_3", remoteId = "g_3", updatedAt = 1000, state = SyncState.SYNCED)
        assertTrue(SyncMerge.resolve(local, remote) is SyncResolution.NoOp)
    }

    @Test
    fun `local edit newer than the server wins - last-write-wins`() {
        val local = record("local-4", remoteId = "g_4", updatedAt = 5000, state = SyncState.PENDING_UPDATE)
        val remote = record("g_4", remoteId = "g_4", updatedAt = 4000, state = SyncState.SYNCED)
        assertTrue(SyncMerge.resolve(local, remote) is SyncResolution.PushLocal)
    }

    @Test
    fun `server edit newer than the unsynced local edit wins - last-write-wins`() {
        val local = record("local-5", remoteId = "g_5", updatedAt = 4000, state = SyncState.PENDING_UPDATE)
        val remote = record("g_5", remoteId = "g_5", updatedAt = 6000, state = SyncState.SYNCED)
        assertTrue(SyncMerge.resolve(local, remote) is SyncResolution.AdoptRemote)
    }

    @Test
    fun `a synced record deleted on the server is dropped locally, not re-pushed`() {
        val local = record("local-6", remoteId = "g_6", updatedAt = 1000, state = SyncState.SYNCED)
        assertTrue(SyncMerge.resolve(local, remote = null) is SyncResolution.NoOp)
    }

    @Test
    fun `resolveAll processes a batch by matching remote ids`() {
        val locals = listOf(
            record("l1", "g1", 100, SyncState.SYNCED),
            record("l2", null, 200, SyncState.PENDING_CREATE),
        )
        val remotes = mapOf("g1" to record("g1", "g1", 100, SyncState.SYNCED))
        val results = SyncMerge.resolveAll(locals, remotes)
        assertEquals(2, results.size)
        assertTrue(results[0] is SyncResolution.NoOp)
        assertTrue(results[1] is SyncResolution.PushLocal)
    }
}
