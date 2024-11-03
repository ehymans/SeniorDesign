// SmsHelper.kt

package com.sur_tec.helmetiq

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsHelper {
    suspend fun sendEmergencySms(
        context: Context,
        contacts: List<Contact>,
        userName: String,
        location: String
    ): Boolean {
        if (contacts.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "No contacts to send SMS to.", Toast.LENGTH_SHORT).show()
            }
            return false
        }

        // Check if SEND_SMS permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "SMS permission not granted.", Toast.LENGTH_SHORT).show()
            }
            return false
        }

        val messageBody = "HelmetIQ: Emergency! Your friend $userName may have been involved in a collision! Their current location is: $location"

        val smsManager = SmsManager.getDefault()
        var success = true

        contacts.forEach { contact ->
            val toNumber = contact.phoneNumber

            try {
                smsManager.sendTextMessage(toNumber, null, messageBody, null, null)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Message sent to ${contact.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                success = false
                withContext(Dispatchers.Main) {
                    val errorMessage = when (e) {
                        is SecurityException -> "Permission denied: ${e.message}"
                        else -> "Error sending SMS: ${e.message}"
                    }
                    Toast.makeText(
                        context,
                        "Error sending message to ${contact.name}: $errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                    e.printStackTrace()
                }
            }
        }
        return success
    }
}
