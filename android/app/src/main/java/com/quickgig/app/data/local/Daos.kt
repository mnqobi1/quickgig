package com.quickgig.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GigDao {
    @Query("SELECT * FROM cached_gigs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CachedGigEntity>>

    @Query("SELECT * FROM cached_gigs WHERE postedBy = :userId OR assignedTo = :userId ORDER BY createdAt DESC")
    fun observeMine(userId: Long): Flow<List<CachedGigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(gigs: List<CachedGigEntity>)

    @Query("DELETE FROM cached_gigs")
    suspend fun clear()
}

@Dao
interface DraftGigDao {
    @Query("SELECT * FROM draft_gigs WHERE pendingSync = 1 ORDER BY createdAtEpochMs ASC")
    fun observePending(): Flow<List<DraftGigEntity>>

    @Insert
    suspend fun insert(draft: DraftGigEntity): Long

    @Update
    suspend fun update(draft: DraftGigEntity)

    @Delete
    suspend fun delete(draft: DraftGigEntity)

    @Query("SELECT COUNT(*) FROM draft_gigs WHERE pendingSync = 1")
    suspend fun pendingCount(): Int
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM cached_messages WHERE gigId = :gigId ORDER BY createdAt ASC")
    fun observeForGig(gigId: Long): Flow<List<CachedMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<CachedMessageEntity>)

    @Query("SELECT * FROM outbox_messages WHERE pendingSync = 1 ORDER BY createdAtEpochMs ASC")
    suspend fun pendingOutbox(): List<OutboxMessageEntity>

    @Insert
    suspend fun queueOutbox(message: OutboxMessageEntity): Long

    @Delete
    suspend fun removeFromOutbox(message: OutboxMessageEntity)
}
