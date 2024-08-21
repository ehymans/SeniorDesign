package com.sur_tec.helmetiq

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun Contacts(navController: NavHostController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Header()

        // New Contact and SMS Toggle
        NewContactAndSmsToggle()

        // Contact List
        ContactList()

        // Bottom Navigation
        BottomNavigation(navController)
    }
}

@Composable
fun Header() {
    Text(
        text = "Emergency Contacts",
        fontSize = 24.sp,
        color = Color.Gray
    )
}

@Composable
fun NewContactAndSmsToggle() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // New Emergency Contact Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "New Emergency Contact",
                fontSize = 18.sp,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { /* TODO: Add new contact */ }) {
               // Icon(
                   // painter = painterResource(id = R.drawable.ic_add), // Replace with your add icon resource
                  //  contentDescription = "Add Contact",
                  //  tint = Color.Gray
                //)
            }
        }

        // Potential Collision SMS Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Potential Collision SMS",
                fontSize = 18.sp,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = false, // Replace with actual state
                onCheckedChange = { /* TODO: Toggle SMS feature */ },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.Gray)
            )
        }
    }
}

@Composable
fun ContactList() {
    Column {
        // Each contact in the list
        val contacts = listOf("Ethan Hymans", "Christopher", "Ana-Victoria Elias", "Hector Agosto", "Chung Yong Chan")
        contacts.forEach { name ->
            ContactItem(name)
        }
    }
}

@Composable
fun ContactItem(name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { /* TODO: Navigate to contact details */ }
    ) {
       // Icon(
           // painter = painterResource(id = R.drawable.ic_contact), // Replace with your contact icon resource
          //  contentDescription = "Contact Icon",
          //  tint = Color.Gray,
          //  modifier = Modifier.size(24.dp)
       // )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = name,
            fontSize = 18.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        //Icon(
           // painter = painterResource(id = R.drawable.ic_arrow_forward), // Replace with your arrow icon resource
           // contentDescription = "Go to Contact",
           // tint = Color.Gray
        //)
    }
}

@Composable
fun BottomNavigation(navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = { navController.navigate("default") },
            modifier = Modifier.background(Color.Cyan)
        ) {
            Text("Home")
        }
        Button(
            onClick = { navController.navigate("contacts") },
            modifier = Modifier.background(Color.Cyan)
        ) {
            Text("SOS Contacts")
        }
    }
}