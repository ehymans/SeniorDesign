// File: Contacts.kt
package com.sur_tec.helmetiq

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.sur_tec.helmetiq.ui.theme.Monnestraut
import kotlinx.coroutines.launch

data class Contact(val name: String, val phoneNumber: String)

@Composable
fun ContactDialog(
    contact: Contact?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phoneDigits by remember {
        mutableStateOf(
            TextFieldValue(
                text = if (contact == null) "" else contact.phoneNumber.filter { it.isDigit() }.takeLast(10),
                selection = TextRange(
                    if (contact == null) 0 else contact.phoneNumber.filter { it.isDigit() }.takeLast(10).length
                )
            )
        )
    }

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
                    value = phoneDigits,
                    onValueChange = { newInput ->
                        val filteredText = newInput.text.filter { it.isDigit() }.take(10)
                        phoneDigits = TextFieldValue(
                            text = filteredText,
                            selection = TextRange(filteredText.length)
                        )
                    },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    visualTransformation = PhoneNumberVisualTransformation(),
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
                        Button(onClick = {
                            val digits = phoneDigits.text
                            val formattedNumber = "+1$digits"
                            onSave(name, formattedNumber)
                        }) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Contacts(navController: NavHostController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val smsPermissionState = rememberPermissionState(permission = Manifest.permission.SEND_SMS)
    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var switchState by remember { mutableStateOf(false) }
    var showNewContactDialog by remember { mutableStateOf(false) }
    var showEditContactDialog by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf(listOf<Contact>()) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }

    var toastMessage by remember { mutableStateOf<String?>(null) }

    val userName = remember { mutableStateOf("") }

    // Load contacts and user name
    LaunchedEffect(Unit) {
        contacts = PrefsHelper.loadContacts(context)
        userName.value = PrefsHelper.loadUserName(context)
    }

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

        // Add the "Test Emergency SMS" button
        Button(
            onClick = {
                when (smsPermissionState.status) {
                    is PermissionStatus.Granted -> {
                        when (locationPermissionState.status) {
                            is PermissionStatus.Granted -> {
                                getLastKnownLocationString(context, fusedLocationClient) { locationString ->
                                    coroutineScope.launch {
                                        val nameToUse = if (userName.value.isNotBlank()) userName.value else "Test User"
                                        val success = SmsHelper.sendEmergencySms(
                                            context,
                                            contacts,
                                            nameToUse,
                                            locationString
                                        )
                                        if (success) {
                                            Toast.makeText(context, "Test SMS sent.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to send Test SMS.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                            is PermissionStatus.Denied -> {
                                locationPermissionState.launchPermissionRequest()
                            }
                        }
                    }
                    is PermissionStatus.Denied -> {
                        smsPermissionState.launchPermissionRequest()
                    }
                }
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Test Emergency SMS")
        }
    }

    if (showNewContactDialog) {
        ContactDialog(
            contact = null,
            onDismiss = { showNewContactDialog = false },
            onSave = { name, phoneNumber ->
                val newContact = Contact(name, phoneNumber)
                contacts = contacts + newContact
                PrefsHelper.saveContacts(context, contacts)
                showNewContactDialog = false
                toastMessage = "Contact Added!"
            },
            onDelete = { }
        )
    }

    if (showEditContactDialog && selectedContact != null) {
        ContactDialog(
            contact = selectedContact,
            onDismiss = { showEditContactDialog = false },
            onSave = { name, phoneNumber ->
                contacts = contacts.map {
                    if (it == selectedContact) Contact(name, phoneNumber) else it
                }
                PrefsHelper.saveContacts(context, contacts)
                showEditContactDialog = false
                toastMessage = "Contact Updated."
            },
            onDelete = {
                contacts = contacts.filter { it != selectedContact }
                PrefsHelper.saveContacts(context, contacts)
                showEditContactDialog = false
                toastMessage = "Contact Deleted."
            }
        )
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            showBottomToast(context, it)
            toastMessage = null
        }
    }
}

@SuppressLint("MissingPermission")
fun getLastKnownLocationString(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocationResult: (String) -> Unit
) {
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        val locationString = if (location != null) {
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            "Location unavailable"
        }
        onLocationResult(locationString)
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
        /*
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
            }*/
    }
}

@Composable
fun ContactList(contacts: List<Contact>, onContactClick: (Contact) -> Unit) {
Column {
if (contacts.isEmpty()) {
    Text(
        text = "No Emergency Contacts Added.",
        fontSize = 16.sp,
        color = Color.Gray,
        modifier = Modifier.padding(16.dp)
    )
} else {
    contacts.forEach { contact ->
        ContactItem(contact, onContactClick)
    }
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

@Composable
fun CustomFloatingActionButton(
onClick: () -> Unit,
backgroundColor: Color = MaterialTheme.colorScheme.primary,
contentColor: Color = MaterialTheme.colorScheme.onPrimary,
icon: @Composable () -> Unit,
shape: CornerBasedShape = MaterialTheme.shapes.small
) {
Box(
contentAlignment = Alignment.Center,
modifier = Modifier
    .padding(end = 12.dp)
    .size(48.dp)
    .clip(shape)
    .background(backgroundColor)
    .clickable {
        onClick()
    }
) {
icon()
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
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.ExtraBold,
    fontStyle = FontStyle.Italic,
    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
)
}
}

fun showBottomToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
Toast.makeText(context, message, duration).apply {
setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
show()
}
}
