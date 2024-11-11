// BluetoothManager.kt

package com.sur_tec.helmetiq

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class BluetoothManager(private val context: Context) {
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private var listenJob: Job? = null
    private var isDisconnecting = false

    companion object {
        private const val DEVICE_NAME = "HelmetIQ"
        private val UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    fun initializeBluetooth(onConnected: () -> Unit) {
        if (coroutineScope.isActive.not()) {
            coroutineScope = CoroutineScope(Dispatchers.IO + Job())
        }

        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                Toast.makeText(context, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
                return
            }

            if (!bluetoothAdapter.isEnabled) {
                Toast.makeText(context, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
                return
            }
            cleanupConnection()

            // Get paired devices
            val pairedDevices = bluetoothAdapter.bondedDevices
            val device = pairedDevices.find { it.name == DEVICE_NAME }

            if (device != null) {
                Toast.makeText(context, "Found HelmetIQ device, attempting to connect...", Toast.LENGTH_SHORT).show()
                connectToDevice(device, onConnected)
            } else {
                Toast.makeText(context, "HelmetIQ device not found. Please pair your helmet first.", Toast.LENGTH_LONG).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
            Log.e("BluetoothManager", "Security Exception", e)
        } catch (e: Exception) {
            Toast.makeText(context, "Bluetooth initialization error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("BluetoothManager", "Initialization error", e)
        }
    }

    private fun connectToDevice(device: BluetoothDevice, onConnected: () -> Unit) {
        coroutineScope.launch {
            try {
                val socket = createBluetoothSocket(device)
                if (socket == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to create Bluetooth socket", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val connected = connectWithRetry(socket, MAX_RETRY_ATTEMPTS)
                if (!connected) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Connection failed after multiple attempts", Toast.LENGTH_SHORT).show()
                    }
                    socket.close()
                    return@launch
                }

                bluetoothSocket = socket
                inputStream = socket.inputStream
                outputStream = socket.outputStream

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Connected to HelmetIQ", Toast.LENGTH_SHORT).show()
                    onConnected()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Connection error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                Log.e("BluetoothManager", "Unexpected error", e)
                cleanupConnection()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket? {
        return withContext(Dispatchers.IO) {
            try {
                device.createRfcommSocketToServiceRecord(UUID)
            } catch (e: IOException) {
                Log.e("BluetoothManager", "Socket creation failed", e)
                null
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectWithRetry(socket: BluetoothSocket, maxAttempts: Int): Boolean {
        repeat(maxAttempts) { attempt ->
            try {
                withContext(Dispatchers.IO) {
                    // Cancel discovery as it may slow down the connection
                    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    bluetoothAdapter?.cancelDiscovery()

                    socket.connect()
                }
                return true
            } catch (e: IOException) {
                Log.e("BluetoothManager", "Connection attempt ${attempt + 1} failed", e)
                if (attempt < maxAttempts - 1) {
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        return false
    }

    private fun hasRequiredPermissions(): Boolean {
        return hasBluetoothPermission() && hasBluetoothAdminPermission()
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasBluetoothAdminPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_ADMIN
        ) == PackageManager.PERMISSION_GRANTED
    }

    // BluetoothManager.kt
    private fun cleanupConnection() {
        isDisconnecting = true
        try {
            listenJob?.cancel()
            listenJob = null

            outputStream?.let {
                try {
                    it.flush()
                    it.close()
                } catch (e: IOException) {
                    Log.e("BluetoothManager", "Error closing output stream", e)
                }
            }

            inputStream?.let {
                try {
                    it.close()
                } catch (e: IOException) {
                    Log.e("BluetoothManager", "Error closing input stream", e)
                }
            }

            bluetoothSocket?.let {
                try {
                    it.close()
                } catch (e: IOException) {
                    Log.e("BluetoothManager", "Error closing socket", e)
                }
            }
        } finally {
            bluetoothSocket = null
            inputStream = null
            outputStream = null
            isDisconnecting = false
        }
    }

    fun disconnect() {
        try {
            isDisconnecting = true
            cleanupConnection()
            coroutineScope.cancel()
            coroutineScope = CoroutineScope(Dispatchers.IO + Job())
            Toast.makeText(context, "Disconnected from HelmetIQ", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(context, "Error disconnecting: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("BluetoothManager", "Could not close the client socket", e)
        } finally {
            isDisconnecting = false
        }
    }

    // BluetoothManager.kt
    fun listenForData(onDataReceived: (String) -> Unit) {
        if (!hasBluetoothPermission()) {
            Toast.makeText(context, "Bluetooth permission required to receive data", Toast.LENGTH_SHORT).show()
            return
        }

        // Cancel any existing listening job
        listenJob?.cancel()

        listenJob = coroutineScope.launch {
            val buffer = ByteArray(1024)

            while (isActive && !isDisconnecting) {
                try {
                    if (bluetoothSocket?.isConnected == true && inputStream != null) {
                        val bytes = inputStream?.read(buffer) ?: -1
                        if (bytes > 0) {
                            val readMessage = String(buffer, 0, bytes)
                            withContext(Dispatchers.Main) {
                                Log.d("BluetoothManager", "Received data: $readMessage")
                                onDataReceived(readMessage)
                            }
                        } else if (bytes == -1) {
                            if (!isDisconnecting) {
                                handleConnectionError()
                            }
                            break
                        }
                    } else {
                        delay(100)
                    }
                } catch (e: IOException) {
                    Log.e("BluetoothManager", "Input stream was disconnected", e)
                    if (!isDisconnecting) {
                        handleConnectionError()
                    }
                    break
                }
            }
        }
    }

    fun sendData(data: String) {
        if (!hasBluetoothPermission()) {
            Toast.makeText(
                context,
                "Bluetooth permission required to send data",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        coroutineScope.launch {
            try {
                if (bluetoothSocket?.isConnected == true && outputStream != null) {
                    outputStream?.write(data.toByteArray())
                    outputStream?.flush()
                } else {
                    Log.e(
                        "BluetoothManager",
                        "Cannot send data - socket is not connected or stream is null"
                    )
                }
            } catch (e: IOException) {
                Log.e("BluetoothManager", "Error occurred when sending data", e)
                if (!isDisconnecting) {
                    handleConnectionError()
                }
            }
        }
    }

    private fun handleConnectionError() {
        coroutineScope.launch(Dispatchers.Main) {
            Toast.makeText(context, "Connection lost. Attempting to reconnect...", Toast.LENGTH_SHORT).show()
            cleanupConnection()
            // Attempt to reconnect
            initializeBluetooth {}
        }
    }
}