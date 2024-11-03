// File: PrefsHelper.kt
package com.sur_tec.helmetiq

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PrefsHelper {
    private const val PREFS_NAME = "helmet_iq_prefs"
    private const val CONTACTS_KEY = "emergency_contacts"
    private const val USER_NAME_KEY = "user_name"
    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveUserName(context: Context, userName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(USER_NAME_KEY, userName).apply()
    }

    fun loadUserName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(USER_NAME_KEY, "") ?: ""
    }

    fun saveContacts(context: Context, contacts: List<Contact>) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        val gson = Gson()
        val json = gson.toJson(contacts)
        editor.putString(CONTACTS_KEY, json)
        editor.apply()
    }

    fun loadContacts(context: Context): List<Contact> {
        val prefs = getPrefs(context)
        val gson = Gson()
        val json = prefs.getString(CONTACTS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<Contact>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
}


