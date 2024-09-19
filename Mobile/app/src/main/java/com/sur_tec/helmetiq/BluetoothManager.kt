package com.sur_tec.helmetiq

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class BluetoothManager(private val context: Context) {

    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @OptIn(ExperimentalPermissionsApi::class)
    fun initializeBluetooth(bluetoothPermissionState: MultiplePermissionsState, onConnected: () -> Unit) {
        // Check for Bluetooth permissions first
        if (!bluetoothPermissionState.allPermissionsGranted) {
            bluetoothPermissionState.launchMultiplePermissionRequest()
            return
        }
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.d("Bluetooth", "Bluetooth is disabled, requesting enable")
            // Request to enable Bluetooth
            context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            return
        }

        // If Bluetooth is enabled, continue to find the ESP32 device
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
        }
        bluetoothDevice = bluetoothAdapter?.bondedDevices?.find { it.name == "ESP32_BT" }
        if (bluetoothDevice != null) {
            Log.d("Bluetooth", "ESP32 device found: ${bluetoothDevice?.name}, ${bluetoothDevice?.address}")
            connectToDevice(bluetoothDevice!!, onConnected)
        } else {
            Log.e("Bluetooth", "ESP32 device not found")
            Toast.makeText(context, "ESP32 device not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToDevice(device: BluetoothDevice, onConnected: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@launch
                }

                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()
                Log.d("Bluetooth", "Connected to ${device.name}")
                withContext(Dispatchers.Main){
                    Toast.makeText(context,"Connected to ${device.name}",Toast.LENGTH_SHORT).show()
                    onConnected()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main){
                    Toast.makeText(context,"Connected failed: ${e.message}",Toast.LENGTH_SHORT).show()
                    Log.e("Bluetooth", "Connection failed", e)
                }
            }
        }
    }

    fun sendData(data: String) {
        bluetoothSocket?.let { socket ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val outputStream = socket.outputStream
                    outputStream.write(data.toByteArray())
                    outputStream.flush()
                    Log.d("Bluetooth", "Data sent: $data")
                } catch (e: Exception) {
                    Log.e("Bluetooth", "Failed to send data", e)
                }
            }
        }
    }

    fun listenForData(onDataReceived: (String) -> Unit) {
        bluetoothSocket?.let { socket ->
            CoroutineScope(Dispatchers.IO).launch {
                val buffer = ByteArray(1024)
                var bytes: Int
                val inputStream = socket.inputStream
                try {
                    while (true) {
                        bytes = inputStream.read(buffer)
                        val receivedData = String(buffer, 0, bytes)
                        Log.d("Bluetooth", "Data received: $receivedData")
                        onDataReceived(receivedData)
                    }
                } catch (e: Exception) {
                    Log.e("Bluetooth", "Failed to receive data", e)
                }
            }
        }
    }

    fun disconnect() {
        try {
            bluetoothSocket?.close()
            Toast.makeText(context,"Disconnected from device",Toast.LENGTH_SHORT).show()
            Log.d("Bluetooth", "Disconnected from device")
        } catch (e: Exception) {
            Log.e("Bluetooth", "Failed to disconnect", e)
        }
    }
}

