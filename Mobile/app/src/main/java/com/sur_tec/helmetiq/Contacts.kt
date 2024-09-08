package com.sur_tec.helmetiq

import android.graphics.drawable.shapes.Shape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sur_tec.helmetiq.navigation.Screens
import com.sur_tec.helmetiq.ui.theme.Monnestraut

@Composable
fun Contacts(navController: NavHostController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Header()

        // New Contact and SMS Toggle
        NewContactAndSmsToggle()

        // Contact List
        ContactList()

        // Bottom Navigation
        //  BottomNavigation(navController)
    }
}

@Composable
fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()

    ) {
        Text(
            text = "Emergency Contacts",
            fontSize = 28.sp,
            fontFamily = Monnestraut,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
        )
    }
}

@Composable
@Preview(showBackground = true)
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
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "New Emergency Contact",
                fontSize = 18.sp,
                color = Color.Gray,
                fontFamily = Monnestraut,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            CustomFloatingActionButton(
                onClick = {},
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium.copy(
                    all = CornerSize(60)
                ),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

            )
        }

        // Potential Collision SMS Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()

                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Potential Collision SMS",
                fontSize = 18.sp,
                fontFamily = Monnestraut,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            Switch(
                checked = false, // Replace with actual state
                onCheckedChange = { /* TODO: Toggle SMS feature */ },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.Gray),
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}

@Composable
fun ContactList() {
    Column {
        // Each contact in the list
        val contacts = listOf(
            "Ethan Hymans",
            "Christopher",
            "Ana-Victoria Elias",
            "Hector Agosto",
            "Chung Yong Chan"
        )
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
fun CustomFloatingActionButton(
    onClick: () -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    icon: @Composable () -> Unit,
    shape: CornerBasedShape = MaterialTheme.shapes.small
) {
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(end = 12.dp)
            .size(48.dp)
            .clip(shape)
            .background(backgroundColor)
            .clickable {
                onClick()
            }) {
        icon()
    }
}

/*
@Composable
fun BottomNavigation(navController: @Composable NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = { navController.navigate(Screens.MAINSCREEN.name) },
            modifier = Modifier.padding(0.dp, 6.dp)
        ) {
            Text("Home")
        }
        Button(
            onClick = { navController.navigate(Screens.CONTACTSSCREEN.name) },
            modifier = Modifier.padding(0.dp, 6.dp)

        ) {
            Text("SOS Contacts")
        }
    }
}*/
