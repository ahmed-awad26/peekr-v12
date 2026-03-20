package com.peekr.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class SecuritySettings(
    val isLockEnabled: Boolean = false,
    val isPinEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val lockAfterSeconds: Int = 30
)

sealed class LockState {
    object Unlocked : LockState()
    object Locked : LockState()
    object BiometricAvailable : LockState()
}

@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "peekr_security_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _lockState = MutableStateFlow<LockState>(LockState.Unlocked)
    val lockState: StateFlow<LockState> = _lockState

    private var lastUnlockTime = 0L

    // ==============================
    // إعدادات الأمان
    // ==============================
    fun getSettings(): SecuritySettings = SecuritySettings(
        isLockEnabled = prefs.getBoolean("lock_enabled", false),
        isPinEnabled = prefs.getBoolean("pin_enabled", false),
        isBiometricEnabled = prefs.getBoolean("biometric_enabled", false),
        lockAfterSeconds = prefs.getInt("lock_after_seconds", 30)
    )

    fun saveLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("lock_enabled", enabled).apply()
        if (!enabled) _lockState.value = LockState.Unlocked
    }

    fun saveLockAfterSeconds(seconds: Int) {
        prefs.edit().putInt("lock_after_seconds", seconds).apply()
    }

    fun saveBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    // ==============================
    // PIN
    // ==============================
    fun setPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val hashed = hashPin(pin)
        prefs.edit()
            .putString("pin_hash", hashed)
            .putBoolean("pin_enabled", true)
            .apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString("pin_hash", null) ?: return false
        return hashPin(pin) == stored
    }

    fun removePin() {
        prefs.edit()
            .remove("pin_hash")
            .putBoolean("pin_enabled", false)
            .apply()
    }

    fun hasPin(): Boolean = prefs.getBoolean("pin_enabled", false) &&
            prefs.getString("pin_hash", null) != null

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    // ==============================
    // بصمة الإصبع
    // ==============================
    fun isBiometricAvailable(): Boolean {
        val bm = BiometricManager.from(context)
        return bm.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean("biometric_enabled", false) && isBiometricAvailable()
    }

    // ==============================
    // حالة القفل
    // ==============================
    fun checkAndLock() {
        val settings = getSettings()
        if (!settings.isLockEnabled) return

        val elapsed = (System.currentTimeMillis() - lastUnlockTime) / 1000
        if (elapsed >= settings.lockAfterSeconds || lastUnlockTime == 0L) {
            _lockState.value = LockState.Locked
        }
    }

    fun onUnlocked() {
        lastUnlockTime = System.currentTimeMillis()
        _lockState.value = LockState.Unlocked
    }

    fun isLocked(): Boolean = _lockState.value == LockState.Locked

    fun isLockEnabled(): Boolean = prefs.getBoolean("lock_enabled", false)
}
