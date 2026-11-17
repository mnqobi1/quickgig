package com.quickgig.app.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickgig.app.data.model.Gig
import com.quickgig.app.data.model.GigCategory
import com.quickgig.app.data.repository.GigRepository
import com.quickgig.app.data.repository.toUiGigs
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class HomeUiState(
    val gigs: List<Gig> = emptyList(),
    val category: GigCategory = GigCategory.ALL,
    val radiusKm: Double = 5.0,
    val isRefreshing: Boolean = false,
    val pendingDraftCount: Int = 0,
)

class HomeViewModel(
    private val gigRepository: GigRepository,
    private val lastKnownLat: Double? = -33.928,
    private val lastKnownLng: Double? = 18.417,
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        gigRepository.observeFeed().toUiGigs().onEach { gigs ->
            uiState = uiState.copy(gigs = applyCategoryFilter(gigs, uiState.category))
        }.launchIn(viewModelScope)

        gigRepository.observePendingDrafts().onEach { drafts ->
            uiState = uiState.copy(pendingDraftCount = drafts.size)
        }.launchIn(viewModelScope)

        refresh()
    }

    fun onCategorySelected(category: GigCategory) {
        uiState = uiState.copy(category = category)
        refresh()
    }

    fun onRadiusSelected(radiusKm: Double) {
        uiState = uiState.copy(radiusKm = radiusKm)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true)
            gigRepository.refreshFeed(
                lat = lastKnownLat, lng = lastKnownLng,
                radiusKm = uiState.radiusKm, category = uiState.category.apiValue,
            )
            uiState = uiState.copy(isRefreshing = false)
        }
    }

    private fun applyCategoryFilter(gigs: List<Gig>, category: GigCategory): List<Gig> =
        if (category == GigCategory.ALL) gigs else gigs.filter { it.category == category.apiValue }
}
