package dev.sebas1705.careereditor.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Injected by MainActivity before first use
lateinit var appContext: Context

actual class SecureStorage : KeyValueStore {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "career_editor_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    actual override fun put(key: String, value: String) = prefs.edit().putString(key, value).apply()
    actual override fun get(key: String): String? = prefs.getString(key, null)
    actual override fun remove(key: String) = prefs.edit().remove(key).apply()
    actual override fun clear() = prefs.edit().clear().apply()
}
