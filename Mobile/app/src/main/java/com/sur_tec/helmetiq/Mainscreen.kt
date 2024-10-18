package com.sur_tec.helmetiq

import android.Manifest
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.sur_tec.helmetiq.navigation.Screens
import com.sur_tec.helmetiq.ui.theme.customColors
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val isConnected = bluetoothViewModel.isConnected.collectAsState()
    val distanceTravelled = bluetoothViewModel.distance.collectAsState().value

    // Bluetooth permissions handling
    val bluetoothPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    )

    // Location permissions handling
    val locationPermissionState =
        rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val cameraPositionState = rememberCameraPositionState()
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    var locationPermissionGranted by remember { mutableStateOf(false) }

    // Collision dialog state
    var showCollisionDialog by remember { mutableStateOf(false) }
    var timer by remember { mutableStateOf(30) }

    // Headlights switch state
    var switchState by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (!bluetoothViewModel.isConnected.value) {  // to prevent constant reconnects
            if (bluetoothPermissionState.allPermissionsGranted) {
                bluetoothViewModel.initializeBluetooth(bluetoothPermissionState) {
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
            }
        }
    }

    // Timer for collision dialog
    LaunchedEffect(showCollisionDialog) {
        if (showCollisionDialog) {
            while (timer > 0) {
                delay(1000L)
                timer -= 1
            }
            if (showCollisionDialog) {
                showCollisionDialog = false
                bluetoothViewModel.sendEmergencySms()
            }
        }
    }

    // Location permission handling
    LaunchedEffect(key1 = locationPermissionState.status.isGranted) {
        if (!locationPermissionGranted) {
            // Ask for location permission
            locationPermissionState.launchPermissionRequest()
        }
        locationPermissionGranted = locationPermissionState.status.isGranted
    }

    if (locationPermissionGranted) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                userLocation = LatLng(it.latitude, it.longitude)
                cameraPositionState.position = CameraPosition.fromLatLngZoom(userLocation!!, 15f)
            } ?: run {
                Toast.makeText(context, "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        LaunchedEffect(key1 = Unit) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderTitle()

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
                    tint = if (isConnected.value) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                if (bluetoothPermissionState.allPermissionsGranted) {
                                    if (!isConnected.value) {
                                        bluetoothViewModel.initializeBluetooth(
                                            bluetoothPermissionState
                                        ) {
                                            Log.d("Bluetooth", "Connected to device")
                                            // Connection status is updated in ViewModel
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
                    if (isConnected.value) {
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
                        onMapLoaded = {
                            // Map loaded
                        }
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
                        text = "Total Distance Traveled: $distanceTravelled Miles",
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
                        text = "Ride Time: 58 Minutes",
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
                // Additional logic if needed
                Toast.makeText(context, "Stay safe!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showCollisionDialog = false
                // Additional logic if needed
                bluetoothViewModel.sendEmergencySms()
            }
        )
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

@Composable
private fun HeaderTitle() {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "HelmetIQ",
            fontSize = 30.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, start = 12.dp)
        )
    }
}

/*on main screen dialog pop-up when collision occurs (a drop for ex)*/
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