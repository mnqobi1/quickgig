package com.quickgig.app.ui.mygigs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickgig.app.data.local.DraftGigEntity
import com.quickgig.app.data.model.Gig
import com.quickgig.app.data.repository.GigRepository
import com.quickgig.app.data.repository.toUiGigs
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

enum class MyGigsTab { POSTED, APPLIED, ACTIVE, COMPLETED }

data class MyGigsUiState(
    val tab: MyGigsTab = MyGigsTab.POSTED,
    val gigs: List<Gig> = emptyList(),
    val drafts: List<DraftGigEntity> = emptyList(),
)

/**
 * The "My Gigs" screen: "a tabbed view: Posted / Applied / Active /
 * Completed" from the Planning doc. Backed by the same offline Room
 * cache as the Home feed, so it still renders (minus anything that
 * never made it to the server) with no connection at all.
 */
class MyGigsViewModel(
    private val gigRepository: GigRepository,
    private val currentUserId: Long,
) : ViewModel() {

    var uiState by mutableStateOf(MyGigsUiState())
        private set

    init {
        combine(
            gigRepository.observeMyGigs(currentUserId).toUiGigs(),
            gigRepository.observePendingDrafts(),
        ) { gigs, drafts -> gigs to drafts }
            .onEach { (gigs, drafts) ->
                uiState = uiState.copy(gigs = filterForTab(gigs, uiState.tab), drafts = drafts)
            }
            .launchIn(viewModelScope)
    }

    fun onTabSelected(tab: MyGigsTab) {
        uiState = uiState.copy(tab = tab)
    }

    private fun filterForTab(gigs: List<Gig>, tab: MyGigsTab): List<Gig> = when (tab) {
        MyGigsTab.POSTED -> gigs.filter { it.postedBy == currentUserId }
        MyGigsTab.APPLIED -> gigs.filter { it.assignedTo != currentUserId && it.postedBy != currentUserId }
        MyGigsTab.ACTIVE -> gigs.filter { it.assignedTo == currentUserId && it.status == "assigned" }
        MyGigsTab.COMPLETED -> gigs.filter { it.status == "completed" }
    }
}
