package id.shoko.arta.core.security

import android.content.Context
import android.content.pm.PackageManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Kunci AES-256-GCM di Android Keystore untuk membungkus passphrase database.
 * Dua alias: tanpa autentikasi, dan dengan autentikasi pengguna (sidik jari / kredensial perangkat).
 */
class KeystoreManager(private val context: Context) {

    private val keyStore: KeyStore by lazy { KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) } }

    fun alias(requireAuth: Boolean) = if (requireAuth) ALIAS_AUTH else ALIAS_PLAIN

    fun hasKey(requireAuth: Boolean): Boolean = keyStore.containsAlias(alias(requireAuth))

    fun deleteKey(requireAuth: Boolean) {
        runCatching { keyStore.deleteEntry(alias(requireAuth)) }
    }

    fun deleteAll() {
        deleteKey(true); deleteKey(false)
    }

    private fun getOrCreateKey(requireAuth: Boolean): SecretKey {
        (keyStore.getKey(alias(requireAuth), null) as? SecretKey)?.let { return it }
        val hasStrongBox = context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        return try {
            generate(requireAuth, strongBox = hasStrongBox)
        } catch (_: StrongBoxUnavailableException) {
            generate(requireAuth, strongBox = false)
        }
    }

    private fun generate(requireAuth: Boolean, strongBox: Boolean): SecretKey {
        val builder = KeyGenParameterSpec.Builder(
            alias(requireAuth),
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .setIsStrongBoxBacked(strongBox)
        if (requireAuth) {
            builder.setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(
                    AUTH_VALIDITY_SECONDS,
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
                )
                .setInvalidatedByBiometricEnrollment(true)
        }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        gen.init(builder.build())
        return gen.generateKey()
    }

    /** @return Pair(iv, ciphertext) */
    fun encrypt(plain: ByteArray, requireAuth: Boolean): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(requireAuth))
        return cipher.iv to cipher.doFinal(plain)
    }

    fun decrypt(iv: ByteArray, cipherText: ByteArray, requireAuth: Boolean): ByteArray {
        val key = keyStore.getKey(alias(requireAuth), null) as? SecretKey
            ?: throw KeyMissingException()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherText)
    }

    class KeyMissingException : Exception("Kunci Keystore tidak ditemukan")

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ALIAS_PLAIN = "arta_db_wrap"
        private const val ALIAS_AUTH = "arta_db_wrap_auth"
        /** Kunci auth dapat dipakai selama N detik setelah BiometricPrompt berhasil. */
        const val AUTH_VALIDITY_SECONDS = 30
    }
}
