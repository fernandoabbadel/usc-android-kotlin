package com.example.usc1.ui.vendor

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MiniVendorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MiniVendorUiState())
    val uiState: StateFlow<MiniVendorUiState> = _uiState.asStateFlow()
}
