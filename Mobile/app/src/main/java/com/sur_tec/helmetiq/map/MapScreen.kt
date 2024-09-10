package com.sur_tec.helmetiq.map

import android.Manifest
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(modifier: Modifier = Modifier,navController: NavController) {

    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val cameraPositionState = rememberCameraPositionState()
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val coroutineScope = rememberCoroutineScope()



    var locationPermissionGranted by remember { mutableStateOf(false) }
    // Check and request location permission


    LaunchedEffect(key1 = locationPermissionState.status.isGranted) {
        if (!locationPermissionGranted) {
            // Ask for location permission
            locationPermissionState.launchPermissionRequest()
        }
        locationPermissionGranted = locationPermissionState.status.isGranted

    }


    val initialLocation = LatLng(-34.0, 151.0) // Default location (Sydney, for example)



    if (locationPermissionGranted){

        fusedLocationClient?.lastLocation?.addOnSuccessListener {location ->
            location?.let {
                userLocation = LatLng(it.latitude,it.longitude)
                cameraPositionState.position = CameraPosition.fromLatLngZoom(userLocation!!,15f)

            }?: run {
                Toast.makeText(context, "Unable to get location", Toast.LENGTH_SHORT).show()
            }

        }
    } else {
        LaunchedEffect(key1 = Unit) {
            locationPermissionState.launchPermissionRequest()
        }
        Text(text = "Location permission required", modifier = Modifier.fillMaxSize())

    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map Screen") }
            )
        }
    ) {

        if (locationPermissionState.status.isGranted){
            GoogleMap(
                modifier = modifier.fillMaxSize(),
                cameraPositionState,
                onMapLoaded = {
                    Toast.makeText(context, "map loaded", Toast.LENGTH_SHORT).show()
                }
            )
        }else {
            Text(text = "Location permission required", modifier = Modifier.fillMaxSize())

        }




    }



}