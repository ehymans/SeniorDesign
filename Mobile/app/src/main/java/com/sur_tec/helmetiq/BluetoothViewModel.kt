package com.sur_tec.helmetiq

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothViewModel: ViewModel() {
    // StateFlow for connection status
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected // Expose it as StateFlow

    // Function to update the connection status
    fun updateConnectionStatus(status: Boolean) {
        _isConnected.value = status
    }
}