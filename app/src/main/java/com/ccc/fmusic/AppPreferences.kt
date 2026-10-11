package com.ccc.fmusic

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

class AppPreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_app_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var serverIp: String
        get() = encryptedPrefs.getString("server_ip", "192.168.1.15") ?: "192.168.1.15"
        set(value) = encryptedPrefs.edit { putString("server_ip", value) }

    var serverPort: Int
        get() = encryptedPrefs.getInt("server_port", 8100)
        set(value) = encryptedPrefs.edit { putInt("server_port", value) }

    var apiKey: String
        get() = encryptedPrefs.getString("api_key", "") ?: ""
        set(value) = encryptedPrefs.edit { putString("api_key", value) }
}