package agentic.triad.missioncontrol.secure

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * The bearer's only home on disk: an Android Keystore-encrypted preference. It is read into memory
 * per request by the MCP client and never logged, never backed up (`allowBackup=false`), never on a
 * crash report. A biometric/device-credential gate belongs in front of [peek] at session start
 * (BiometricPrompt) — wired at the connection sheet.
 */
class BearerVault(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "triad_bearer",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun store(bearer: String) = prefs.edit().putString(KEY, bearer).apply()
    fun peek(): String? = prefs.getString(KEY, null)
    fun clear() = prefs.edit().remove(KEY).apply()

    private companion object { const val KEY = "bearer" }
}
