// BluetoothManager.kt

package com.sur_tec.helmetiq

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothManager(private val context: Context) {

    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val deviceName = "HelmetIQ" // Replace with your device name
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Replace with your UUID

    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    fun initializeBluetooth(onConnected: () -> Unit) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Log.e("BluetoothManager", "Bluetooth is not supported on this device")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            // Handle Bluetooth not enabled scenario
            Log.e("BluetoothManager", "Bluetooth is not enabled")
            return
        }

        val device = bluetoothAdapter.bondedDevices.find { it.name == deviceName }

        if (device != null) {
            connectToDevice(device, onConnected)
        } else {
            Log.e("BluetoothManager", "Device not found")
        }
    }

    private fun connectToDevice(device: BluetoothDevice, onConnected: () -> Unit) {
        coroutineScope.launch {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()
                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream

                withContext(Dispatchers.Main) {
                    onConnected()
                }
            } catch (e: IOException) {
                Log.e("BluetoothManager", "Connection failed", e)
            }
        }
    }

    fun listenForData(onDataReceived: (String) -> Unit) {
        coroutineScope.launch {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (true) {
                try {
                    bytes = inputStream?.read(buffer) ?: break
                    val readMessage = String(buffer, 0, bytes)
                    withContext(Dispatchers.Main) {
                        onDataReceived(readMessage)
                    }
                } catch (e: IOException) {
                    Log.e("BluetoothManager", "Input stream was disconnected", e)
                    break
                }
            }
        }
    }

    fun sendData(data: String) {
        coroutineScope.launch {
            try {
                outputStream?.write(data.toByteArray())
            } catch (e: IOException) {
                Log.e("BluetoothManager", "Error occurred when sending data", e)
            }
        }
    }

    fun disconnect() {
        try {
            bluetoothSocket?.close()
            inputStream = null
            outputStream = null
            bluetoothSocket = null
            coroutineScope.cancel()
        } catch (e: IOException) {
            Log.e("BluetoothManager", "Could not close the client socket", e)
        }
    }
}
