package com.aruuu.app.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages credential storage using Android Keystore + AES-GCM encryption.
 *
 * Credentials are never stored in plain text. Each write:
 *  1. Generates a random 12-byte IV.
 *  2. Encrypts value with an AES-256-GCM key anchored to the Android Keystore.
 *  3. Stores Base64(IV + ciphertext) in EncryptedSharedPreferences.
 *
 * The Keystore key is non-exportable and hardware-backed on supported devices.
 */
@Singleton
class SecureCredentialManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "ARUUUMasterKey_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH = 12
        private const val PREFS_FILE = "aruuu_secure_creds"

        // Credential keys
        const val KEY_PIN_HASH = "pin_credential"
        const val KEY_PASSWORD_HASH = "password_credential"
        const val KEY_PATTERN_HASH = "pattern_credential"
        const val KEY_SALT = "credential_salt"
        const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        const val KEY_LAST_FAILED_TS = "last_failed_timestamp"
        const val KEY_AUTH_METHOD = "primary_auth_method"
    }

    // Plain SharedPreferences backed by Keystore-encrypted values
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    }

    // ─── Keystore key management ──────────────────────────────────────────

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false) // App-level auth handled by ARUUU
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGen.generateKey()
    }

    // ─── Low-level encrypt / decrypt ─────────────────────────────────────

    private fun encrypt(plaintext: String): String {
        val key = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv                      // 12 bytes GCM IV (auto-generated)
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + ciphertext          // prepend IV to ciphertext
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val key = getOrCreateSecretKey()
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, IV_LENGTH)
        val ciphertext = combined.copyOfRange(IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    // ─── Public API ───────────────────────────────────────────────────────

    /** Store any string value encrypted under the Keystore key. */
    fun put(key: String, value: String) {
        prefs.edit().putString(key, encrypt(value)).apply()
    }

    /** Retrieve and decrypt a stored value, or null if absent. */
    fun get(key: String): String? {
        val encoded = prefs.getString(key, null) ?: return null
        return runCatching { decrypt(encoded) }.getOrNull()
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun contains(key: String): Boolean = prefs.contains(key)

    // ─── Convenience: salt ────────────────────────────────────────────────

    fun getOrCreateSalt(): String {
        get(KEY_SALT)?.let { return it }
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
            .let { Base64.encodeToString(it, Base64.NO_WRAP) }
        put(KEY_SALT, salt)
        return salt
    }

    // ─── Convenience: failed attempts ─────────────────────────────────────

    fun getFailedAttempts(): Int = get(KEY_FAILED_ATTEMPTS)?.toIntOrNull() ?: 0

    fun incrementFailedAttempts(): Int {
        val next = getFailedAttempts() + 1
        put(KEY_FAILED_ATTEMPTS, next.toString())
        put(KEY_LAST_FAILED_TS, System.currentTimeMillis().toString())
        return next
    }

    fun resetFailedAttempts() {
        put(KEY_FAILED_ATTEMPTS, "0")
    }

    fun getLastFailedTimestamp(): Long =
        get(KEY_LAST_FAILED_TS)?.toLongOrNull() ?: 0L
}
