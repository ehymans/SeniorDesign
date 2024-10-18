// SmsHelper.kt
package com.sur_tec.helmetiq

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

// SmsHelper.kt
object SmsHelper {
    suspend fun sendEmergencySms(context: Context, contacts: List<Contact>) {
        if (contacts.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "No contacts to send SMS to.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val accountSid = "xxx"
        val authToken = "xxx"
        val fromNumber = "xxx"


        val messageBody = "HelmetIQ Midterm Demo: Emergency! The user may have been involved in a collision!"
        val client = OkHttpClient()

        contacts.forEach { contact ->
            val toNumber = contact.phoneNumber

            val url = "https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json"

            val formBody = FormBody.Builder()
                .add("To", toNumber)
                .add("From", fromNumber)
                .add("Body", messageBody)
                .build()

            val credential = Credentials.basic(accountSid, authToken)

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .header("Authorization", credential)
                .build()

            try {
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Failed to send message to ${contact.name}: ${response.code} - $responseBody",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Message sent to ${contact.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = when (e) {
                        is IOException -> "Network error: ${e.message}"
                        is IllegalArgumentException -> "Invalid argument: ${e.message}"
                        else -> "Unexpected error: ${e.javaClass.simpleName} - ${e.message}"
                    }
                    Toast.makeText(
                        context,
                        "Error sending message to ${contact.name}: $errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                    e.printStackTrace() // This will print the full stack trace to logcat
                }
            }
        }
    }
}
