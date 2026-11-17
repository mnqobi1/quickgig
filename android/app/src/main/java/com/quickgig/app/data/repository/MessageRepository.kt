package com.quickgig.app.data.repository

import com.quickgig.app.data.local.CachedMessageEntity
import com.quickgig.app.data.local.MessageDao
import com.quickgig.app.data.local.OutboxMessageEntity
import com.quickgig.app.data.remote.NetworkMonitor
import com.quickgig.app.data.remote.QuickGigApi
import com.quickgig.app.data.remote.SendMessageRequest
import kotlinx.coroutines.flow.Flow

class MessageRepository(
    private val api: QuickGigApi,
    private val messageDao: MessageDao,
    private val networkMonitor: NetworkMonitor,
) {
    fun observeMessages(gigId: Long): Flow<List<CachedMessageEntity>> = messageDao.observeForGig(gigId)

    suspend fun refresh(gigId: Long) {
        if (!networkMonitor.isOnline()) return
        runCatching { api.getMessages(gigId) }.onSuccess { response ->
            val messages = response.body()?.get("messages") ?: return@onSuccess
            messageDao.upsertAll(
                messages.map {
                    CachedMessageEntity(it.id, it.gigId, it.senderId, it.recipientId, it.body, it.createdAt)
                }
            )
        }
    }

    suspend fun send(gigId: Long, recipientId: Long, body: String) {
        if (!networkMonitor.isOnline()) {
            messageDao.queueOutbox(
                OutboxMessageEntity(gigId = gigId, recipientId = recipientId, body = body, createdAtEpochMs = System.currentTimeMillis())
            )
            return
        }
        runCatching { api.sendMessage(gigId, SendMessageRequest(recipientId, body)) }
            .onFailure {
                messageDao.queueOutbox(
                    OutboxMessageEntity(gigId = gigId, recipientId = recipientId, body = body, createdAtEpochMs = System.currentTimeMillis())
                )
            }
    }

    suspend fun trySyncOutbox(message: OutboxMessageEntity): Boolean =
        runCatching { api.sendMessage(message.gigId, SendMessageRequest(message.recipientId, message.body)) }
            .map { it.isSuccessful }
            .getOrDefault(false)
}
