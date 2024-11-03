// Mainscreen.kt

package com.sur_tec.helmetiq

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.sur_tec.helmetiq.navigation.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Mainscreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    bluetoothViewModel: BluetoothViewModel
) {
    val context = LocalContext.current
    val isConnected by bluetoothViewModel.isConnected.collectAsState()

    // Permission states
    val smsPermissionState = rememberPermissionState(permission = Manifest.permission.SEND_SMS)
    val bluetoothPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    )
    val locationPermissionState =
        rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)

    val cameraPositionState = rememberCameraPositionState()
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var showCollisionDialog by remember { mutableStateOf(false) }
    var timer by remember { mutableStateOf(30) }
    var switchState by rememberSaveable { mutableStateOf(true) }

    var userName by remember { mutableStateOf("") }
    var showUserNameDialog by remember { mutableStateOf(false) }

    var pendingSendEmergencySms by remember { mutableStateOf(false) }

    // Load user name
    LaunchedEffect(Unit) {
        userName = PrefsHelper.loadUserName(context)
        if (userName.isBlank()) {
            showUserNameDialog = true
        }
    }

    // Function to initiate sending emergency SMS
    fun onSendEmergencySms() {
        pendingSendEmergencySms = true
    }

    // Handle sending SMS and permissions
    LaunchedEffect(pendingSendEmergencySms, smsPermissionState.status, locationPermissionState.status) {
        if (pendingSendEmergencySms) {
            if (smsPermissionState.status is PermissionStatus.Granted &&
                locationPermissionState.status is PermissionStatus.Granted
            ) {
                sendEmergencySms(context, bluetoothViewModel, userName, fusedLocationClient)
                pendingSendEmergencySms = false
            } else {
                if (smsPermissionState.status is PermissionStatus.Denied) {
                    smsPermissionState.launchPermissionRequest()
                }
                if (locationPermissionState.status is PermissionStatus.Denied) {
                    locationPermissionState.launchPermissionRequest()
                }
            }
        }
    }

    // Initialize Bluetooth
    LaunchedEffect(Unit) {
        if (!isConnected) {
            if (bluetoothPermissionState.allPermissionsGranted) {
                bluetoothViewModel.initializeBluetooth {
                    Log.d("Bluetooth", "Connected to device")
                }
            } else {
                bluetoothPermissionState.launchMultiplePermissionRequest()
            }
        }
    }

    // Observe Bluetooth events for collision detection
    LaunchedEffect(Unit) {
        bluetoothViewModel.eventFlow.collectLatest { event ->
            when (event) {
                is BluetoothViewModel.BluetoothEvent.CollisionDetected -> {
                    showCollisionDialog = true
                    timer = 10
                }
                else -> { /* Handle other events if necessary */ }
            }
        }
    }

    // Timer for collision dialog
    LaunchedEffect(showCollisionDialog) {
        if (showCollisionDialog) {
            timer = 10
            while (timer > 0) {
                delay(1000L)
                timer -= 1
            }
            if (showCollisionDialog) {
                showCollisionDialog = false
                onSendEmergencySms()
            }
        }
    }

    // Location permission handling
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            getLastKnownLocation(context, fusedLocationClient) { location ->
                location?.let {
                    userLocation = LatLng(it.latitude, it.longitude)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(userLocation!!, 15f)
                } ?: run {
                    Toast.makeText(context, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    // UI Components
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderTitle(userName)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Helmet Image
            HelmetImage()

            // Battery and Bluetooth Icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_battery),
                    contentDescription = "Battery",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(40.dp)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_bluetooth),
                    contentDescription = "Bluetooth",
                    tint = if (isConnected) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                if (bluetoothPermissionState.allPermissionsGranted) {
                                    if (!isConnected) {
                                        bluetoothViewModel.initializeBluetooth {
                                            Log.d("Bluetooth", "Connected to device")
                                        }
                                    } else {
                                        bluetoothViewModel.disconnectBluetooth()
                                        Log.d("Bluetooth", "Disconnected")
                                    }
                                } else {
                                    bluetoothPermissionState.launchMultiplePermissionRequest()
                                }
                            }
                        )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Headlights Toggle
            HeadLight(switchState) {
                switchState = it
                if (bluetoothPermissionState.allPermissionsGranted) {
                    if (isConnected) {
                        // Send appropriate message based on switchState
                        if (switchState) {
                            bluetoothViewModel.bluetoothManager.sendData("1")
                        } else {
                            bluetoothViewModel.bluetoothManager.sendData("2")
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Not connected to any device to send data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    bluetoothPermissionState.launchMultiplePermissionRequest()
                }
            }

            // Map layout
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .clickable {
                        navController.navigate(Screens.MAPSCREEN.name)
                    },
            ) {
                if (locationPermissionState.status.isGranted) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        onMapLoaded = { /* Map loaded */ }
                    )
                } else {
                    Text(
                        text = "Location permission required",
                        modifier = Modifier.fillMaxSize(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            MaterialTheme.colorScheme.primary
                        )
                ) {
                    Text(
                        text = "Total Distance Traveled: N/A",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            MaterialTheme.colorScheme.primary
                        )
                ) {
                    Text(
                        text = "Ride Time: N/A",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    // Collision Dialog
    if (showCollisionDialog) {
        CollisionDialog(
            timer = timer,
            onConfirm = {
                showCollisionDialog = false
                Toast.makeText(context, "Stay safe!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showCollisionDialog = false
                onSendEmergencySms()
            }
        )
    }

    // User Name Dialog
    if (showUserNameDialog) {
        UserNameDialog(
            onSave = { name ->
                userName = name
                PrefsHelper.saveUserName(context, name)
                showUserNameDialog = false
            },
            onDismiss = {
                userName = "User"
                PrefsHelper.saveUserName(context, userName)
                showUserNameDialog = false
            }
        )
    }
}

@SuppressLint("MissingPermission")
fun sendEmergencySms(
    context: Context,
    bluetoothViewModel: BluetoothViewModel,
    userName: String,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
) {
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        val locationString = if (location != null) {
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            "Location unavailable"
        }
        bluetoothViewModel.sendEmergencySms(userName, locationString) { success ->
            if (success) {
                Toast.makeText(context, "Emergency SMS sent.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to send Emergency SMS.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun getLastKnownLocation(
    context: Context,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocationResult: (android.location.Location?) -> Unit
) {
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        onLocationResult(location)
    }
}

@Composable
fun UserNameDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Enter Your Name",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { newValue -> name = newValue },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name)
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun HeadLight(switchState: Boolean = true, onSwitchChanged: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Headlights",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = switchState,
            onCheckedChange = onSwitchChanged,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Cyan,
                uncheckedThumbColor = Color.Gray,
                checkedTrackColor = Color.DarkGray,
                uncheckedTrackColor = Color.LightGray,
            )
        )
    }
}

@Composable
private fun HelmetImage() {
    Image(
        painter = painterResource(id = R.drawable.helmet_man_removebg),
        contentDescription = "Helmet",
        modifier = Modifier.size(200.dp),
        contentScale = ContentScale.Crop
    )
}

// Updated HeaderTitle to accept userName
@Composable
private fun HeaderTitle(userName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "HelmetIQ - $userName",
            fontSize = 30.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, start = 12.dp)
        )
    }
}

/* Collision dialog when a possible collision is detected */
@Composable
fun CollisionDialog(
    timer: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissing by clicking outside */ },
        title = { Text("Possible Collision Detected") },
        text = { Text("Were you involved in a collision? ($timer seconds left)") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("No")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Yes, send SMS")
            }
        }
    )
}
