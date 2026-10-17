package dev.sebas1705.careereditor.security

/**
 * Platform-specific secure key-value store.
 * Android: EncryptedSharedPreferences (AES256)
 */
expect class SecureStorage() {
    fun put(key: String, value: String)
    fun get(key: String): String?
    fun remove(key: String)
    fun clear()
}

object SecureKeys {
    const val API_TOKEN = "api_token"
    const val API_BASE_URL = "api_base_url"
}
