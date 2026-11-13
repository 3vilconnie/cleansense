package com.cleansense.app.data

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Cuentas locales guardadas solo en este teléfono (SharedPreferences).
 * Las contraseñas nunca se guardan en texto plano: se guarda una sal aleatoria
 * y el hash PBKDF2 de la contraseña.
 */
class UserRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun userExists(username: String): Boolean = prefs.contains(key(username))

    fun createUser(username: String, password: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = hash(password, salt)
        prefs.edit { putString(key(username), encode(salt) + ":" + encode(hash)) }
    }

    fun checkPassword(username: String, password: String): Boolean {
        val stored = prefs.getString(key(username), null) ?: return false
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = decode(parts[0])
        val expected = decode(parts[1])
        return MessageDigest.isEqual(expected, hash(password, salt))
    }

    private fun key(username: String) = "user:" + username.trim().lowercase(Locale.ROOT)

    private fun hash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(text: String) = Base64.decode(text, Base64.NO_WRAP)

    private companion object {
        const val PREFS_NAME = "cleansense_users"
        const val SALT_BYTES = 16
        const val ITERATIONS = 10_000
        const val KEY_BITS = 256
    }
}
