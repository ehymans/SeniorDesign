package com.sur_tec.helmetiq

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun Mainscreen(navController: NavHostController, modifier: Modifier = Modifier) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "HelmetIQ",
            fontSize = 24.sp,
            color = Color.Gray
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Helmet Image
            // Image(
            //painter = painterResource(id = R.drawable.helmet_image), // Replace with your image resource
            //contentDescription = "Helmet",
            //modifier = Modifier.size(120.dp),
            // contentScale = ContentScale.Fit
            //)
            Spacer(modifier = Modifier.height(8.dp))
            // Battery and Bluetooth Icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon(painter = painterResource(id = R.drawable.ic_battery), contentDescription = "Battery", tint = Color.Gray)
                //Icon(painter = painterResource(id = R.drawable.ic_bluetooth), contentDescription = "Bluetooth", tint = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Headlights Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Headlights", fontSize = 18.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = false, // Replace with actual state
                    onCheckedChange = { /* TODO: Add logic here */ },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Gray)
                )
            }
        }

        // Placeholder for Map, replace with an actual map implementation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            BasicText(text = "Map View Placeholder")
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Distance Traveled: 10 Miles",
                fontSize = 18.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ride Time: 58 Minutes",
                fontSize = 18.sp,
                color = Color.Gray
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(

                onClick = { navController.navigate("default") }

            ) {
                Text("Home")
            }
            Button(
                onClick = { navController.navigate("contacts") }

            ) {
                Text("SOS Contacts")
            }
        }
    }
}