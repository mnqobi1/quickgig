package com.quickgig.app.ui.postgig

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickgig.app.R
import com.quickgig.app.data.model.GeoPoint
import com.quickgig.app.data.model.GigCategory
import com.quickgig.app.data.repository.GigRepository
import com.quickgig.app.data.repository.PostGigResult
import kotlinx.coroutines.launch

data class PostGigUiState(
    val title: String = "",
    val description: String = "",
    val category: GigCategory = GigCategory.ERRANDS,
    val priceCredits: String = "",
    val isSubmitting: Boolean = false,
    /** A string resource id rather than a raw message, so the error
     * re-renders correctly if the user switches language mid-flow. */
    val errorRes: Int? = null,
    val result: PostGigResult? = null,
)

class PostGigViewModel(
    private val gigRepository: GigRepository,
    private val currentLat: Double? = -33.928,
    private val currentLng: Double? = 18.417,
) : ViewModel() {

    var uiState by mutableStateOf(PostGigUiState())
        private set

    fun onTitleChange(value: String) { uiState = uiState.copy(title = value) }
    fun onDescriptionChange(value: String) { uiState = uiState.copy(description = value) }
    fun onCategorySelected(category: GigCategory) { uiState = uiState.copy(category = category) }
    fun onPriceChange(value: String) { uiState = uiState.copy(priceCredits = value.filter { it.isDigit() }) }

    fun submit() {
        if (uiState.title.isBlank()) {
            uiState = uiState.copy(errorRes = R.string.post_gig_missing_title)
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true, errorRes = null)
            val result = gigRepository.postGig(
                title = uiState.title,
                category = uiState.category.apiValue,
                description = uiState.description.ifBlank { null },
                priceCredits = uiState.priceCredits.toIntOrNull() ?: 0,
                location = GeoPoint(currentLat, currentLng),
                neededBy = null,
            )
            uiState = uiState.copy(isSubmitting = false, result = result)
        }
    }
}
