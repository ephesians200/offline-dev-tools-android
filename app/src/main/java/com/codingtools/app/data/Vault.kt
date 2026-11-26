package com.codingtools.app.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import com.codingtools.app.tools.Json

/** AES-256-GCM using a key that never leaves the Android Keystore. */
object Vault {
    private const val ALIAS = "codingtools_vault_key"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build()
        )
        return gen.generateKey()
    }

    /** Deletes the Keystore key (used by "Erase vault" so a fresh, valid key is created on the next save). */
    fun resetKey() { runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(ALIAS) } }

    fun encrypt(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        val out = c.iv + c.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun decrypt(b64: String): String {
        val b = Base64.decode(b64, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, b.copyOfRange(0, 12)))
        return String(c.doFinal(b.copyOfRange(12, b.size)), Charsets.UTF_8)
    }

    /**
     * Decrypts and parses the stored vault. A failure (for example the Keystore key was invalidated)
     * is returned as a failed Result and must NEVER be treated as "no notes": saving over it would
     * destroy the still-recoverable ciphertext.
     */
    fun load(raw: String): Result<List<Map<String, String>>> = runCatching {
        if (raw.isBlank()) emptyList()
        else (Json.parse(decrypt(raw)) as List<*>).map { n -> (n as Map<*, *>).entries.associate { it.key.toString() to it.value.toString() } }
    }
}
