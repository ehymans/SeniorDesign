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

    // StateFlow for distance (initialized to 0.0)
    private val _distance = MutableStateFlow("0")
    val distance: StateFlow<String> = _distance // Expose it as StateFlow

    // Function to update the connection status
    fun updateConnectionStatus(status: Boolean) {
        _isConnected.value = status
    }

    // Function to update the distance
    fun updateDistance(newDistance: String) {
        _distance.value = newDistance
    }
}