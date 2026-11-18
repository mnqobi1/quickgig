package com.quickgig.app.ui.gigdetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickgig.app.data.model.Gig
import com.quickgig.app.data.model.PublicProfile
import com.quickgig.app.data.repository.GigRepository
import com.quickgig.app.data.repository.UserRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class GigDetailUiState(
    val gig: Gig? = null,
    val poster: PublicProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isApplying: Boolean = false,
    val isUpdatingStatus: Boolean = false,
    val ratingStars: Int = 0,
    val onTime: Boolean = true,
    val ratingSubmitted: Boolean = false,
)

class GigDetailViewModel(
    private val gigRepository: GigRepository,
    private val userRepository: UserRepository,
    val gigId: Long,
    val currentUserId: Long,
) : ViewModel() {

    var uiState by mutableStateOf(GigDetailUiState())
        private set

    init {
        loadGig()
    }

    private fun loadGig() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val result = gigRepository.getGig(gigId)
            if (result.isSuccess) {
                val gig = result.getOrNull()!!
                uiState = uiState.copy(gig = gig, isLoading = false)
                if (gig.postedBy != currentUserId) {
                    loadPoster(gig.postedBy)
                }
            } else {
                uiState = uiState.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Failed to load gig")
            }
        }
    }

    private fun loadPoster(posterId: Long) {
        viewModelScope.launch {
            val result = userRepository.getProfile(posterId)
            if (result.isSuccess) {
                uiState = uiState.copy(poster = result.getOrNull()!!)
            }
        }
    }

    fun onApply() {
        val gig = uiState.gig ?: return
        uiState = uiState.copy(isApplying = true)
        viewModelScope.launch {
            val result = gigRepository.applyToGig(gigId)
            uiState = uiState.copy(isApplying = false)
            if (result.isSuccess) {
                loadGig()
            } else {
                uiState = uiState.copy(error = result.exceptionOrNull()?.message ?: "Couldn't apply")
            }
        }
    }

    fun onAssign(applicantId: Long) {
        uiState = uiState.copy(isUpdatingStatus = true)
        viewModelScope.launch {
            val result = gigRepository.updateStatus(gigId, "assigned", applicantId, null, null)
            uiState = uiState.copy(isUpdatingStatus = false)
            if (result.isSuccess) {
                loadGig()
            } else {
                uiState = uiState.copy(error = result.exceptionOrNull()?.message ?: "Couldn't assign gig")
            }
        }
    }

    fun onComplete(stars: Int, onTime: Boolean) {
        uiState = uiState.copy(isUpdatingStatus = true)
        viewModelScope.launch {
            val result = gigRepository.updateStatus(gigId, "completed", null, stars, onTime)
            uiState = uiState.copy(isUpdatingStatus = false, ratingSubmitted = true)
            if (result.isSuccess) {
                loadGig()
            } else {
                uiState = uiState.copy(error = result.exceptionOrNull()?.message ?: "Couldn't complete gig")
            }
        }
    }

    fun onCancel() {
        uiState = uiState.copy(isUpdatingStatus = true)
        viewModelScope.launch {
            val result = gigRepository.updateStatus(gigId, "cancelled", null, null, null)
            uiState = uiState.copy(isUpdatingStatus = false)
            if (result.isSuccess) {
                loadGig()
            } else {
                uiState = uiState.copy(error = result.exceptionOrNull()?.message ?: "Couldn't cancel gig")
            }
        }
    }

    fun onWithdraw() {
        uiState = uiState.copy(isUpdatingStatus = true)
        viewModelScope.launch {
            val result = gigRepository.withdrawFromGig(gigId)
            uiState = uiState.copy(isUpdatingStatus = false)
            if (result.isSuccess) {
                loadGig()
            } else {
                uiState = uiState.copy(error = result.exceptionOrNull()?.message ?: "Couldn't withdraw")
            }
        }
    }

    fun onReopen() {
        uiState = uiState.copy(isUpdatingStatus = true)
        viewModelScope.launch {
            val result = gigRepository.updateStatus(gigId, "open", null, null, null)
            uiState = uiState.copy(isUpdatingStatus = false)
            if (result.isSuccess) {
                loadGig()
            } else {
                uiState = uiState.copy(error = result.exceptionOrNull()?.message ?: "Couldn't reopen gig")
            }
        }
    }

    fun onRatingChanged(stars: Int) {
        uiState = uiState.copy(ratingStars = stars)
    }

    fun onOnTimeChanged(onTime: Boolean) {
        uiState = uiState.copy(onTime = onTime)
    }
}
