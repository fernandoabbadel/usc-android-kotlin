package com.example.usc1.ui.community

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CommunityViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    fun selectTab(tab: String) {
        _uiState.update { current ->
            current.copy(
                activeTab = tab,
                posts = if (tab == "Todos") {
                    CommunityMockData.posts
                } else {
                    CommunityMockData.posts.filter { it.category == tab }
                },
            )
        }
    }
}
