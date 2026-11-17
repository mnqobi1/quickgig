package com.quickgig.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_gigs")
data class CachedGigEntity(
    @PrimaryKey val remoteId: Long,
    val title: String,
    val description: String?,
    val category: String,
    val priceCredits: Int,
    val lat: Double?,
    val lng: Double?,
    val neededBy: String?,
    val status: String,
    val postedBy: Long,
    val assignedTo: Long?,
    val createdAt: String,
    val updatedAtEpochMs: Long,
)

@Entity(tableName = "draft_gigs")
data class DraftGigEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val title: String,
    val description: String?,
    val category: String,
    val priceCredits: Int,
    val lat: Double?,
    val lng: Double?,
    val neededBy: String?,
    val createdAtEpochMs: Long,
    val pendingSync: Boolean = true,
    val syncError: String? = null,
)

@Entity(tableName = "cached_messages")
data class CachedMessageEntity(
    @PrimaryKey val remoteId: Long,
    val gigId: Long,
    val senderId: Long,
    val recipientId: Long,
    val body: String,
    val createdAt: String,
)

@Entity(tableName = "outbox_messages")
data class OutboxMessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val gigId: Long,
    val recipientId: Long,
    val body: String,
    val createdAtEpochMs: Long,
    val pendingSync: Boolean = true,
)
