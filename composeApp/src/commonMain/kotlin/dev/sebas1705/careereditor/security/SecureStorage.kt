package dev.sebas1705.careereditor.security

/** Key-value store the repository depends on, so tests can swap in an in-memory one. */
interface KeyValueStore {
    fun put(key: String, value: String)
    fun get(key: String): String?
    fun remove(key: String)
    fun clear()
}

/**
 * Platform-specific secure key-value store.
 * Android: EncryptedSharedPreferences (AES256)
 */
expect class SecureStorage() : KeyValueStore {
    override fun put(key: String, value: String)
    override fun get(key: String): String?
    override fun remove(key: String)
    override fun clear()
}

object SecureKeys {
    const val API_TOKEN = "api_token"
    const val API_BASE_URL = "api_base_url"
}
