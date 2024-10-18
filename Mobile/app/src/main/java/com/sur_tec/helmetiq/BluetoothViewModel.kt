// BluetoothViewModel.kt

package com.sur_tec.helmetiq

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {
    // Instance of BluetoothManager
    val bluetoothManager = BluetoothManager(application.applicationContext)

    // StateFlow for connection status
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    // StateFlow for distance
    private val _distance = MutableStateFlow("0")
    val distance: StateFlow<String> = _distance

    // Function to update the connection status
    fun updateConnectionStatus(status: Boolean) {
        _isConnected.value = status
    }

    // Function to update the distance
    fun updateDistance(newDistance: String) {
        _distance.value = newDistance
    }

    @OptIn(ExperimentalPermissionsApi::class)
    fun initializeBluetooth(
        bluetoothPermissionState: MultiplePermissionsState,
        onConnected: () -> Unit
    ) {
        bluetoothManager.initializeBluetooth(bluetoothPermissionState) {
            // On connected
            updateConnectionStatus(true)
            listenForData()
            onConnected()
        }
    }

    private fun listenForData() {
        bluetoothManager.listenForData { data ->
            updateDistance(data)
            // You can add additional data handling here
        }
    }

    fun disconnectBluetooth() {
        bluetoothManager.disconnect()
        updateConnectionStatus(false)
    }
}
