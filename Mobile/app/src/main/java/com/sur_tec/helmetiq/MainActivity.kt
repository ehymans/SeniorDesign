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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.sur_tec.helmetiq.ui.theme.HelmetIQTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID


class MainActivity : ComponentActivity() {
    private lateinit var bluetoothViewModel: BluetoothViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bluetoothViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[BluetoothViewModel::class.java]
        setContent {
            HelmetIQApp {
                HelmetIQNavigation(bluetoothViewModel)
            }
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
