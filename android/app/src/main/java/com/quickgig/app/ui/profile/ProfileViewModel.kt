package com.quickgig.app.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickgig.app.data.model.PublicProfile
import com.quickgig.app.data.repository.UserRepository
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: PublicProfile? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/** Profile screen: "Hustle Score, badges, star rating, completed-gig
 * history, and the 'Verified Student' indicator" from the Planning doc. */
class ProfileViewModel(
    private val userRepository: UserRepository,
    private val userId: Long,
) : ViewModel() {

    var uiState by mutableStateOf(ProfileUiState())
        private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            userRepository.getProfile(userId).fold(
                onSuccess = { uiState = uiState.copy(isLoading = false, profile = it) },
                onFailure = { uiState = uiState.copy(isLoading = false, error = it.message) },
            )
        }
    }
}
