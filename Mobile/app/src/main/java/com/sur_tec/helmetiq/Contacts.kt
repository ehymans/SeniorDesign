package com.sur_tec.helmetiq

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.sur_tec.helmetiq.ui.theme.Monnestraut

data class Contact(val name: String, val phoneNumber: String)

@Composable
fun ContactDialog(
    contact: Contact?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phoneNumber by remember { mutableStateOf(contact?.phoneNumber ?: "") }

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
                    text = if (contact == null) "New Emergency Contact" else "Edit Emergency Contact",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (contact != null) {
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    } else {
                        Spacer(Modifier.width(0.dp))
                    }
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { onSave(name, phoneNumber) }) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Contacts(navController: NavHostController, modifier: Modifier = Modifier) {
    var switchState by remember { mutableStateOf(false) }
    var showNewContactDialog by remember { mutableStateOf(false) }
    var showEditContactDialog by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf(listOf<Contact>()) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header()

        NewContactAndSmsToggle(
            switchState = switchState,
            onCheckChanged = { switchState = it },
            onNewContactClick = { showNewContactDialog = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        ContactList(
            contacts = contacts,
            onContactClick = { contact ->
                selectedContact = contact
                showEditContactDialog = true
            }
        )
    }

    if (showNewContactDialog) {
        ContactDialog(
            contact = null,
            onDismiss = { showNewContactDialog = false },
            onSave = { name, phoneNumber ->
                contacts = contacts + Contact(name, phoneNumber)
                showNewContactDialog = false
            },
            onDelete = { } // Not used for new contacts
        )
    }

    if (showEditContactDialog && selectedContact != null) {
        ContactDialog(
            contact = selectedContact,
            onDismiss = { showEditContactDialog = false },
            onSave = { name, phoneNumber ->
                contacts = contacts.map { if (it == selectedContact) Contact(name, phoneNumber) else it }
                showEditContactDialog = false
            },
            onDelete = {
                contacts = contacts.filter { it != selectedContact }
                showEditContactDialog = false
            }
        )
    }
}

@Composable
fun NewContactAndSmsToggle(
    switchState: Boolean,
    onCheckChanged: (Boolean) -> Unit,
    onNewContactClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
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
                onClick = onNewContactClick,
                backgroundColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shape = MaterialTheme.shapes.medium.copy(all = CornerSize(60)),
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
                checked = switchState,
                onCheckedChange = onCheckChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Cyan,
                    uncheckedThumbColor = Color.Gray,
                    checkedTrackColor = Color.DarkGray,
                    uncheckedTrackColor = Color.LightGray,
                ),
                modifier = Modifier.padding(end = 12.dp, start = 12.dp)
            )
        }
    }
}

@Composable
fun ContactList(contacts: List<Contact>, onContactClick: (Contact) -> Unit) {
    Column {
        contacts.forEach { contact ->
            ContactItem(contact, onContactClick)
        }
    }
}

@Composable
fun ContactItem(contact: Contact, onClick: (Contact) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(vertical = 12.dp, horizontal = 12.dp)
            .clickable { onClick(contact) }
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Contact Icon",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                fontSize = 18.sp,
                fontFamily = Monnestraut,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = contact.phoneNumber,
                fontSize = 14.sp,
                fontFamily = Monnestraut,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
        }
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Go to Contact",
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}
// SAVE THIS FUNCTION!
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

// SAVE THIS FUNCTION!
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
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
        )
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


/*
@Composable
@Preview(showBackground = true)
fun PreviewfunNewContactAndSmsToggle() {
    NewContactAndSmsToggle(false) {

    }
}*/