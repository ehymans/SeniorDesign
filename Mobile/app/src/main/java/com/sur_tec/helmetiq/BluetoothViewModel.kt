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
    val bluetoothManager = BluetoothManager(application.applicationContext)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    sealed class BluetoothEvent {
        object CollisionDetected : BluetoothEvent()
        object SmsPermissionDenied : BluetoothEvent()
    }

    private val _eventFlow = MutableSharedFlow<BluetoothEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // Remove automatic initialization
    private var isBluetoothInitialized = false

    fun updateConnectionStatus(status: Boolean) {
        _isConnected.value = status
    }

    // Function to explicitly connect when button is pressed
    fun connectBluetooth(onConnected: () -> Unit) {
        bluetoothManager.initializeBluetooth {
            updateConnectionStatus(true)
            listenForData()
            onConnected()
        }
    }

    fun disconnectBluetooth() {
        bluetoothManager.disconnect()
        updateConnectionStatus(false)
        isBluetoothInitialized = false
    }

    // function that listens to receive the collision string sent from the ESP32
    private fun listenForData() {
        bluetoothManager.listenForData { data ->
            if (data.trim() == "69") {
                viewModelScope.launch {
                    _eventFlow.emit(BluetoothEvent.CollisionDetected)
                }
            }
        }
    }
}

/*
// unneeded function from when some Bluetooth things were handling SMS functionality
    fun sendEmergencySms(userName: String, location: String, onSmsSent: (Boolean) -> Unit) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val contacts = PrefsHelper.loadContacts(context)
            val result = SmsHelper.sendEmergencySms(context, contacts, userName, location)
            onSmsSent(result)
        }
    }
 */
