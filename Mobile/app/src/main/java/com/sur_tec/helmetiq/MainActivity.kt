package com.sur_tec.helmetiq

import HelmetIQNavigation
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.sur_tec.helmetiq.ui.theme.HelmetIQTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private lateinit var bluetoothSocket: BluetoothSocket
    private var device: BluetoothDevice? = null
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HelmetIQApp {
                HelmetIQNavigation {
                    // Check for Bluetooth support
                    if (bluetoothAdapter == null) {
                        Log.d("Bluetooth", "Device doesn't support Bluetooth")
                        Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show()
                        return@HelmetIQNavigation
                    }

                    // Check and request Bluetooth permissions
                    if (checkBluetoothPermissions()) {
                        initializeBluetooth()
                    } else {
                        requestBluetoothPermissions()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeBluetooth()
            } else {
                Log.d("Bluetooth", "Bluetooth permissions denied")
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initialize Bluetooth Socket and connect
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@launch
                }
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket.connect()

                Log.d("Bluetooth", "Connected to ${device.name}")
                listenForData(bluetoothSocket.inputStream)

                // Send data to ESP32
                sendData(bluetoothSocket.outputStream, "Hello from Android!")
            } catch (e: Exception) {
                Log.e("Bluetooth", "Connection failed", e)
            }
        }
    }

    private fun sendData(outputStream: OutputStream, data: String) {
        try {
            outputStream.write(data.toByteArray())
            outputStream.flush()
            Log.d("Bluetooth", "Data sent: $data")
        } catch (e: Exception) {
            Log.e("Bluetooth", "Failed to send data", e)
        }
    }

    private fun listenForData(inputStream: InputStream) {
        val buffer = ByteArray(1024)
        var bytes: Int

        while (true) {
            try {
                bytes = inputStream.read(buffer)
                val receivedData = String(buffer, 0, bytes)
                Log.d("Bluetooth", "Data received: $receivedData")
            } catch (e: Exception) {
                Log.e("Bluetooth", "Failed to receive data", e)
                break
            }
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
        return permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        ActivityCompat.requestPermissions(this, permissions, REQUEST_BLUETOOTH_PERMISSIONS)
    }

    private fun initializeBluetooth() {
        if (!bluetoothAdapter!!.isEnabled) {
            Log.d("Bluetooth", "Bluetooth is disabled, requesting enable")
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1)
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Search for the device with name "ESP32_BT"
        device = bluetoothAdapter?.bondedDevices?.find { it.name == "ESP32_BT" }

        if (device != null) {
            Log.d("Bluetooth", "ESP32 device found: ${device?.name}, ${device?.address}")
            connectToDevice(device!!)
        } else {
            Log.e("Bluetooth", "ESP32 device not found")
            Toast.makeText(this, "ESP32 device not found", Toast.LENGTH_SHORT).show()
        }
    }
}





@Composable
fun HelmetIQApp(content: @Composable () -> Unit) {
    HelmetIQTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {

            content()
        }
    }
}

/*  @Composable
  fun Greeting(name: String, modifier: Modifier = Modifier) {
      Text(
          text = "Hello $name!",
          modifier = modifier
      )
  }

  @Preview(showBackground = true)
  @Composable
  fun GreetingPreview() {
      HelmetIQTheme {
          Greeting("Android")
      }
  }
}

 */