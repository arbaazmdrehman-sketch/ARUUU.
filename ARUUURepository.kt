package com.aruuu.app.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.aruuu.app.data.local.*
import com.aruuu.app.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "aruuu_settings")

/**
 * Central repository.
 *
 *  • App list queries go through PackageManager + Room.
 *  • Credentials go through SecureCredentialManager (Keystore-backed).
 *  • User preferences go through DataStore.
 */
@Singleton
class ARUUURepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lockedAppDao: LockedAppDao,
    private val intruderDao: IntruderDao,
    private val credentials: SecureCredentialManager,
) {

    // ─── DataStore preference keys ────────────────────────────────────────

    private object Keys {
        val AUTH_METHOD = stringPreferencesKey("auth_method")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val FACE_UNLOCK_ENABLED = booleanPreferencesKey("face_unlock_enabled")
        val AUTO_LOCK_SECONDS = intPreferencesKey("auto_lock_seconds")
        val INTRUDER_SELFIE = booleanPreferencesKey("intruder_selfie")
        val MAX_FAILED = intPreferencesKey("max_failed_attempts")
        val DISGUISE_ENABLED = booleanPreferencesKey("disguise_enabled")
        val DISGUISE_LABEL = stringPreferencesKey("disguise_label")
        val HAPTIC = booleanPreferencesKey("haptic_feedback")
        val NOTIF_BADGE = booleanPreferencesKey("show_notif_badge")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_complete")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LAST_BACKUP = longPreferencesKey("last_backup_ts")
    }

    // ─── Settings ─────────────────────────────────────────────────────────

    val settings: Flow<VaultSettings> = context.dataStore.data.map { prefs ->
        VaultSettings(
            primaryAuthMethod = AuthMethod.valueOf(
                prefs[Keys.AUTH_METHOD] ?: AuthMethod.PIN.name
            ),
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            faceUnlockEnabled = prefs[Keys.FACE_UNLOCK_ENABLED] ?: false,
            autoLockDelaySeconds = prefs[Keys.AUTO_LOCK_SECONDS] ?: 30,
            intruderSelfieEnabled = prefs[Keys.INTRUDER_SELFIE] ?: true,
            maxFailedAttempts = prefs[Keys.MAX_FAILED] ?: 3,
            disguiseModeEnabled = prefs[Keys.DISGUISE_ENABLED] ?: false,
            disguiseLabel = prefs[Keys.DISGUISE_LABEL] ?: "Calculator",
            hapticFeedback = prefs[Keys.HAPTIC] ?: true,
            showNotificationBadge = prefs[Keys.NOTIF_BADGE] ?: false,
            onboardingComplete = prefs[Keys.ONBOARDING_DONE] ?: false,
            themeMode = ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name),
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP] ?: 0L,
        )
    }.catch { emit(VaultSettings()) }

    suspend fun updateSettings(update: VaultSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTH_METHOD] = update.primaryAuthMethod.name
            prefs[Keys.BIOMETRIC_ENABLED] = update.biometricEnabled
            prefs[Keys.FACE_UNLOCK_ENABLED] = update.faceUnlockEnabled
            prefs[Keys.AUTO_LOCK_SECONDS] = update.autoLockDelaySeconds
            prefs[Keys.INTRUDER_SELFIE] = update.intruderSelfieEnabled
            prefs[Keys.MAX_FAILED] = update.maxFailedAttempts
            prefs[Keys.DISGUISE_ENABLED] = update.disguiseModeEnabled
            prefs[Keys.DISGUISE_LABEL] = update.disguiseLabel
            prefs[Keys.HAPTIC] = update.hapticFeedback
            prefs[Keys.NOTIF_BADGE] = update.showNotificationBadge
            prefs[Keys.ONBOARDING_DONE] = update.onboardingComplete
            prefs[Keys.THEME_MODE] = update.themeMode.name
            prefs[Keys.LAST_BACKUP] = update.lastBackupTimestamp
        }
    }

    suspend fun markOnboardingComplete() {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    // ─── Installed apps ───────────────────────────────────────────────────

    suspend fun getInstalledApps(includeSystem: Boolean = false): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val lockedPkgs = lockedAppDao.getAll().associate { it.packageName to it }

            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { info ->
                    val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (!includeSystem && isSystem) return@filter false
                    info.packageName != context.packageName // exclude ARUUU itself
                }
                .mapNotNull { info ->
                    runCatching {
                        val label = pm.getApplicationLabel(info).toString()
                        val pkgInfo = pm.getPackageInfo(info.packageName, 0)
                        val locked = lockedPkgs[info.packageName]
                        AppInfo(
                            packageName = info.packageName,
                            appName = label,
                            versionName = pkgInfo.versionName ?: "",
                            versionCode = pkgInfo.longVersionCode,
                            isSystemApp = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                            isHidden = locked?.isHidden ?: false,
                            isLocked = locked?.isLocked ?: false,
                            installedAt = pkgInfo.firstInstallTime,
                        )
                    }.getOrNull()
                }
                .sortedBy { it.appName.lowercase() }
        }

    // ─── Locked/Hidden apps ───────────────────────────────────────────────

    fun observeLockedApps(): Flow<List<LockedAppEntity>> = lockedAppDao.observeAll()

    suspend fun addToVault(pkg: String, appName: String, hidden: Boolean) {
        lockedAppDao.insert(
            LockedAppEntity(
                packageName = pkg,
                appName = appName,
                isHidden = hidden,
                isLocked = true,
            )
        )
    }

    suspend fun removeFromVault(pkg: String) = lockedAppDao.deleteByPackage(pkg)

    suspend fun isLocked(pkg: String) = lockedAppDao.isLocked(pkg)
    suspend fun isHidden(pkg: String) = lockedAppDao.isHidden(pkg)
    suspend fun updateLastAccessed(pkg: String) = lockedAppDao.updateLastAccessed(pkg)

    // ─── Credential management ────────────────────────────────────────────

    /** Hash input with stored salt using SHA-256. */
    private fun hash(input: String): String {
        val salt = credentials.getOrCreateSalt()
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest("$salt:$input".toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun setPin(pin: String) = credentials.put(SecureCredentialManager.KEY_PIN_HASH, hash(pin))
    fun verifyPin(pin: String): Boolean {
        val stored = credentials.get(SecureCredentialManager.KEY_PIN_HASH) ?: return false
        return stored == hash(pin)
    }
    fun hasPinSet(): Boolean = credentials.contains(SecureCredentialManager.KEY_PIN_HASH)

    fun setPassword(pw: String) = credentials.put(SecureCredentialManager.KEY_PASSWORD_HASH, hash(pw))
    fun verifyPassword(pw: String): Boolean {
        val stored = credentials.get(SecureCredentialManager.KEY_PASSWORD_HASH) ?: return false
        return stored == hash(pw)
    }
    fun hasPasswordSet(): Boolean = credentials.contains(SecureCredentialManager.KEY_PASSWORD_HASH)

    fun setPattern(points: List<Int>) = credentials.put(
        SecureCredentialManager.KEY_PATTERN_HASH,
        hash(points.joinToString(","))
    )
    fun verifyPattern(points: List<Int>): Boolean {
        val stored = credentials.get(SecureCredentialManager.KEY_PATTERN_HASH) ?: return false
        return stored == hash(points.joinToString(","))
    }
    fun hasPatternSet(): Boolean = credentials.contains(SecureCredentialManager.KEY_PATTERN_HASH)

    // ─── Failed attempts ──────────────────────────────────────────────────

    fun getFailedAttempts() = credentials.getFailedAttempts()
    fun incrementFailedAttempts() = credentials.incrementFailedAttempts()
    fun resetFailedAttempts() = credentials.resetFailedAttempts()
    fun getLastFailedTimestamp() = credentials.getLastFailedTimestamp()

    // ─── Intruder records ─────────────────────────────────────────────────

    fun observeIntruderRecords(): Flow<List<IntruderEntity>> = intruderDao.observeAll()

    suspend fun saveIntruderRecord(imagePath: String, failedAttempts: Int, targetPkg: String = "") {
        intruderDao.insert(
            IntruderEntity(
                timestampMs = System.currentTimeMillis(),
                imagePath = imagePath,
                failedAttempts = failedAttempts,
                targetPackage = targetPkg,
            )
        )
    }

    suspend fun deleteIntruderRecord(id: Long) = intruderDao.delete(id)
    suspend fun clearIntruderRecords() = intruderDao.deleteAll()

    // ─── Nuke everything (factory reset) ─────────────────────────────────

    suspend fun factoryReset() {
        lockedAppDao.deleteAll()
        intruderDao.deleteAll()
        credentials.clear()
        context.dataStore.edit { it.clear() }
    }
}
