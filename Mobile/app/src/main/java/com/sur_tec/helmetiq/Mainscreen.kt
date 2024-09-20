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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Mainscreen(navController: NavHostController, modifier: Modifier = Modifier,bluetoothViewModel: BluetoothViewModel) {

    val context = LocalContext.current
    val isConnected = bluetoothViewModel.isConnected.collectAsState() // Observing the connection state
    val distanceTravelled=bluetoothViewModel.distance.collectAsState().value
    // Initialize BluetoothManager
    val bluetoothManager = remember { BluetoothManager(context) }

    // Bluetooth permissions handling
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

    var locationPermissionGranted by remember { mutableStateOf(false) }


    //UPDATE DISTANCE TRAVELLED
    LaunchedEffect (distanceTravelled){
        if(bluetoothPermissionState.allPermissionsGranted){
            if(isConnected.value){
                bluetoothManager.listenForData { data->
                    bluetoothViewModel.updateDistance(data)
                }
            }
//            else{
//                Toast.makeText(context,"Not connected to any device to receive data",Toast.LENGTH_SHORT).show()
//            }
        }
        else{
            Toast.makeText(context,"Don't have permission to receive data",Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(key1 = locationPermissionState.status.isGranted) {
        if (!locationPermissionGranted) {
            // Ask for location permission
            locationPermissionState.launchPermissionRequest()
        }
        locationPermissionGranted = locationPermissionState.status.isGranted

    }

    if (locationPermissionGranted) {

        fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
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
        Text(text = "Location permission required", modifier = Modifier.fillMaxSize())

    }

    var switchState by rememberSaveable {
        mutableStateOf(false)
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
                    tint = MaterialTheme.colorScheme.secondary,  // Use a bold primary color
                    modifier = Modifier.size(40.dp)  // Larger icon size
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_bluetooth),
                    contentDescription = "Bluetooth",
                    tint = if(isConnected.value)MaterialTheme.colorScheme.primary else Color.Gray,  // Use a contrasting color
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                if (bluetoothPermissionState.allPermissionsGranted) {
                                    if (!isConnected.value) {
                                        bluetoothManager.initializeBluetooth(
                                            bluetoothPermissionState
                                        ) {
                                            // Update connection state in ViewModel
                                            Log.d("Bluetooth", "Connected to device")
                                            bluetoothViewModel.updateConnectionStatus(true)
                                            bluetoothManager.listenForData { data ->
                                                bluetoothViewModel.updateDistance(data)
                                            }
                                        }
                                    } else {
                                        bluetoothManager.disconnect()
                                        bluetoothViewModel.updateConnectionStatus(false) // Update connection state in ViewModel
                                        Log.d("Bluetooth", "Disconnected")
                                        bluetoothManager.listenForData { data ->
                                            bluetoothViewModel.updateDistance(data)
                                        }
                                    }
                                } else {
                                    bluetoothPermissionState.launchMultiplePermissionRequest()
                                }
                            }
                        )  // Larger icon size
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Headlights Toggle
            HeadLight(switchState) {
                switchState = it
                if(bluetoothPermissionState.allPermissionsGranted){
                    if(isConnected.value){
                        bluetoothManager.sendData("Headlights are on")
                    }
                    else{
                        Toast.makeText(context,"Not connected to any device to send data",Toast.LENGTH_SHORT).show()
                    }
                }
                else{
                    Toast.makeText(context,"Don't have permission to send data",Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Placeholder for Map, replace with an actual map implementation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clickable {
                    navController.navigate(Screens.MAPSCREEN.name)
                },
        ) {
            if (locationPermissionState.status.isGranted) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState,
                    onMapLoaded = {
                        Toast.makeText(context, "map loaded", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Text(text = "Location permission required", modifier = Modifier.fillMaxSize())

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
                    color =  MaterialTheme.colorScheme.onPrimary,
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

@Composable
private fun HeadLight(switchState: Boolean = false, onSwitchChanged: (Boolean) -> Unit) {
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
                checkedThumbColor = Color.Cyan, // Thumb color when switch is ON
                uncheckedThumbColor = Color.Gray, // Thumb color when switch is OFF
                checkedTrackColor = Color.DarkGray, // Track color when switch is ON
                uncheckedTrackColor = Color.LightGray, // Track color when switch is OFF
            )
        )
    }
}


@Composable
@Preview
private fun HelmetImage() {
    Image(
        painter = painterResource(id = R.drawable.helmet_man_removebg), // Replace with your image resource
        contentDescription = "Helmet",
        modifier = Modifier.size(200.dp),
        contentScale = ContentScale.Crop
    )
}

@Composable
@Preview(showBackground = true)
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
            fontWeight = FontWeight.Black, // Use heavy, bold font for modern design
            fontFamily = FontFamily.SansSerif, // Switch to modern sans-serif fonts
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, start = 12.dp)

        )
    }
}

