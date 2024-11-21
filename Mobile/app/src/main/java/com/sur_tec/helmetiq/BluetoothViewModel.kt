// BluetoothViewModel.kt

package com.sur_tec.helmetiq

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {
    val bluetoothManager = BluetoothManager(application.applicationContext)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var isListening = false

    // RIDE METRICS
    private val _totalRideTime = MutableStateFlow(0L) // in seconds
    val totalRideTime: StateFlow<Long> = _totalRideTime

    private val _totalDistance = MutableStateFlow(0f) // in meters
    val totalDistance: StateFlow<Float> = _totalDistance

    private var timerJob: Job? = null
    private var locationJob: Job? = null
    private var locationCallback: LocationCallback? = null

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)
    private var lastLocation: Location? = null
    // END OF RIDE METRICS

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
        if (status) {
            startTracking()
        } else {
            stopTracking()
        }
    }

    // New function to set up collision detection
    private fun setupCollisionDetection() {
        viewModelScope.launch {
            try {
                bluetoothManager.listenForData { data ->
                    if (data.trim() == "69") {
                        viewModelScope.launch {
                            _eventFlow.emit(BluetoothEvent.CollisionDetected)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Error setting up collision detection", e)
            }
        }
    }

    // Function to explicitly connect when button is pressed
    fun connectBluetooth(onConnected: () -> Unit) {
        bluetoothManager.initializeBluetooth {
            updateConnectionStatus(true)
            //listenForData()
            //startListening()
            setupCollisionDetection()
            //startTracking()
            onConnected()
        }
    }

    // Add this helper function
    private fun startListening() {
        if (!isListening) {
            isListening = true
            listenForData()
        }
    }

    // added a cleanup function 11/11/24
    private fun cleanup() {
        updateConnectionStatus(false)
        stopTracking()
        bluetoothManager.stopListening()
        // Cancel any existing coroutines or jobs if necessary
        isListening = false
    }

    fun disconnectBluetooth() {
        cleanup()
        bluetoothManager.disconnect()
        isBluetoothInitialized = false
    }

    private fun startTracking() {
        startTimer()
        startLocationTracking()
    }

    private fun stopTracking() {
        stopTimer()
        stopLocationTracking()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while(isActive) {
                delay(1000)
                _totalRideTime.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        locationJob?.cancel()
        stopLocationUpdates() // Remove any existing callbacks

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                lastLocation?.let { last ->
                    val distance = location?.distanceTo(last)
                    if (distance != null) {
                        _totalDistance.value += distance
                    }
                }
                lastLocation = location
            }
        }

        locationJob = viewModelScope.launch {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 5000 // Update every 5 seconds
            }

            locationCallback?.let { callback ->
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )
            }
        }
    }

    private fun stopLocationTracking() {
        locationJob?.cancel()
        locationJob = null
        stopLocationUpdates()
        lastLocation = null
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
        locationCallback = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
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
