// BluetoothViewModel.kt

package com.sur_tec.helmetiq

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {
    // Instance of BluetoothManager
    val bluetoothManager = BluetoothManager(application.applicationContext)

    // StateFlow for connection status
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    sealed class BluetoothEvent {
        object CollisionDetected : BluetoothEvent()
        object SmsPermissionDenied : BluetoothEvent()
    }

    private val _eventFlow = MutableSharedFlow<BluetoothEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // Function to update the connection status
    fun updateConnectionStatus(status: Boolean) {
        _isConnected.value = status
    }

    private var isBluetoothInitialized = false

    @OptIn(ExperimentalPermissionsApi::class)
    fun initializeBluetooth(
        onConnected: () -> Unit
    ) {
        if (isBluetoothInitialized) return
        isBluetoothInitialized = true
        bluetoothManager.initializeBluetooth {
            // On connected
            updateConnectionStatus(true)
            listenForData()
            onConnected()
        }
    }

    // Updated for collision flag
    private fun listenForData() {
        bluetoothManager.listenForData { data ->
            if (data.trim() == "69") {
                viewModelScope.launch {
                    _eventFlow.emit(BluetoothEvent.CollisionDetected)
                }
            }
        }
    }

    fun sendEmergencySms(userName: String, location: String, onSmsSent: (Boolean) -> Unit) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val contacts = PrefsHelper.loadContacts(context)
            val result = SmsHelper.sendEmergencySms(context, contacts, userName, location)
            onSmsSent(result)
        }
    }

    fun disconnectBluetooth() {
        bluetoothManager.disconnect()
        updateConnectionStatus(false)
    }
}
