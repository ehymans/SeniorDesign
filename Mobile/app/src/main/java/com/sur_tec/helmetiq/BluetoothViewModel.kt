// BluetoothViewModel.kt

package com.sur_tec.helmetiq

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
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

    // StateFlow for distance
    //private val _distance = MutableStateFlow("0")
    //val distance: StateFlow<String> = _distance

    sealed class BluetoothEvent
    {
        object CollisionDetected : BluetoothEvent()
    }

    private val _eventFlow = MutableSharedFlow<BluetoothEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // Function to update the connection status
    fun updateConnectionStatus(status: Boolean) {
        _isConnected.value = status
    }

    /*
    // Function to update the distance
    fun updateDistance(newDistance: String) {
        _distance.value = newDistance
    }
    */


    private var isBluetoothInitialized = false

    @OptIn(ExperimentalPermissionsApi::class)
    fun initializeBluetooth(
        bluetoothPermissionState: MultiplePermissionsState,
        onConnected: () -> Unit
    ) {
        if(isBluetoothInitialized) return
        isBluetoothInitialized = true
        bluetoothManager.initializeBluetooth(bluetoothPermissionState) {
            // On connected
            updateConnectionStatus(true)
            listenForData()
            onConnected()
        }
    }

    // updated for collision flag 10/18
    private fun listenForData() {
        bluetoothManager.listenForData { data ->
            if(data.trim() == "69")
            {
                viewModelScope.launch {
                    _eventFlow.emit(BluetoothEvent.CollisionDetected)
                }
            }
        }
    }

    fun sendEmergencySms() {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val contacts = PrefsHelper.loadContacts(context)
            SmsHelper.sendEmergencySms(context, contacts)
        }
    }

    fun disconnectBluetooth() {
        bluetoothManager.disconnect()
        updateConnectionStatus(false)
    }
}
