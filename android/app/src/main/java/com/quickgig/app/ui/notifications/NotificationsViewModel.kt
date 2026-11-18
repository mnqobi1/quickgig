package com.quickgig.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.quickgig.app.data.model.Notification
import com.quickgig.app.data.repository.UserRepository
import com.quickgig.app.data.repository.toNotification
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class NotificationsViewModel(
    private val userRepository: UserRepository,
    private val userId: Long,
) : ViewModel() {

    var uiState by mutableStateOf(NotificationsUiState())
        private set

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val result = userRepository.fetchNotifications()
            uiState = uiState.copy(isLoading = false)
            val raw = result.getOrNull()
            if (raw != null) {
                val list = (raw["notifications"] as? List<Map<*, *>> ?: emptyList())
                    .mapNotNull { it as? Map<*, *> }
                    .map { it.toNotification() }
                uiState = uiState.copy(
                    notifications = list,
                    unreadCount = list.count { !it.read },
                )
            } else {
                uiState = uiState.copy(error = result.exceptionOrNull()?.message ?: "Failed to load notifications")
            }
        }
    }

    fun markRead(notificationId: Long) {
        viewModelScope.launch {
            userRepository.markNotificationRead(ids = listOf(notificationId))
            loadNotifications()
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            userRepository.markNotificationRead()
            loadNotifications()
        }
    }

    fun refresh() {
        loadNotifications()
    }
}

