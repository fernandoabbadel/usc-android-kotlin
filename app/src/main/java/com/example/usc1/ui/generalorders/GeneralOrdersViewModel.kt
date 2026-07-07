package com.example.usc1.ui.generalorders

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GeneralOrdersViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GeneralOrdersUiState())
    val uiState: StateFlow<GeneralOrdersUiState> = _uiState.asStateFlow()

    fun selectStatus(status: GeneralOrderStatus?) {
        _uiState.update { it.copy(selectedStatus = status) }
    }
}
