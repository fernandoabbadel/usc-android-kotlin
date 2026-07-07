package com.example.usc1.ui.membershipCard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MembershipCardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MembershipCardUiState())
    val uiState: StateFlow<MembershipCardUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            delay(350)
            _uiState.value = MembershipCardUiState()
        }
    }
}
