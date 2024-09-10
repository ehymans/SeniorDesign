package com.sur_tec.helmetiq

import android.graphics.drawable.shapes.Shape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sur_tec.helmetiq.navigation.Screens
import com.sur_tec.helmetiq.ui.theme.Monnestraut
import com.sur_tec.helmetiq.ui.theme.customColors

@Composable
fun Contacts(navController: NavHostController, modifier: Modifier = Modifier) {

    var switchState by rememberSaveable {
        mutableStateOf(false)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Header()

        // New Contact and SMS Toggle
        NewContactAndSmsToggle(switchState) {
            switchState = it
        }

        Spacer(modifier = Modifier.height(24.dp))
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
            fontSize = 24.sp,
            fontFamily = Monnestraut,
            color = customColors.primary,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
        )
    }
}

@Composable
@Preview(showBackground = true)
fun NewContactAndSmsToggle(switchState: Boolean = false, onCheckChanged: (Boolean) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // New Emergency Contact Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "New Emergency Contact",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onPrimary,
                fontFamily = Monnestraut,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            CustomFloatingActionButton(
                onClick = {},
                backgroundColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
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
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Potential Collision SMS",
                fontSize = 18.sp,
                fontFamily = Monnestraut,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            Switch(
                checked = switchState, // Replace with actual state
                onCheckedChange = {
                    onCheckChanged(it)
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.Cyan),
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
@Preview(showBackground = true)
fun ContactItem(name: String = "salman") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(vertical = 12.dp, horizontal = 12.dp)
            .clickable { /* TODO: Navigate to contact details */ }
    ) {
        Icon(
            imageVector = Icons.Default.Person, // Replace with your contact icon resource
            contentDescription = "Contact Icon",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = name,
            fontSize = 18.sp,
            fontFamily = Monnestraut,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ArrowForward, // Replace with your arrow icon resource
            contentDescription = "Go to Contact",

            tint = MaterialTheme.colorScheme.onPrimary
        )
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


@Composable
@Preview(showBackground = true)
fun PreviewfunNewContactAndSmsToggle() {
    NewContactAndSmsToggle(false) {

    }
}