package com.quickgig.app.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickgig.app.data.model.ChatMessage
import com.quickgig.app.data.repository.MessageRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentUserId: Long = -1,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(
    private val messageRepository: MessageRepository,
    private val gigId: Long,
    private val currentUserId: Long,
) : ViewModel() {

    var uiState by mutableStateOf(ChatUiState(currentUserId = currentUserId))
        private set

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            messageRepository.refresh(gigId)
            messageRepository.observeMessages(gigId)
                .onEach { entities ->
                    uiState = uiState.copy(
                        messages = entities.map { e ->
                            ChatMessage(
                                id = e.remoteId,
                                gigId = e.gigId,
                                senderId = e.senderId,
                                recipientId = e.recipientId,
                                body = e.body,
                                createdAt = e.createdAt,
                            )
                        },
                        isLoading = false,
                    )
                }
                .launchIn(viewModelScope)
        }
    }

    fun send(body: String) {
        viewModelScope.launch {
            messageRepository.send(gigId = gigId, recipientId = resolveRecipientId(), body = body)
            messageRepository.refresh(gigId)
        }
    }

    /** The chat participant who is not the current user. Falls back to current user
     * (the API determines the real recipient automatically from the gig's poster/assignee). */
    private fun resolveRecipientId(): Long {
        return if (uiState.messages.any { it.senderId != currentUserId }) {
            uiState.messages.first { it.senderId != currentUserId }.senderId
        } else {
            currentUserId
        }
    }
}
