package id.shoko.arta.core.security

import android.content.Context
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.SecureRandom

/**
 * Passphrase DB: 32 byte acak, dienkripsi AES-GCM dengan kunci Keystore.
 * Disimpan di file privat aplikasi: [versi][flagAuth][len IV][IV][len CT][CT].
 */
class PassphraseStore(context: Context, private val keystore: KeystoreManager) {

    private val file = File(context.noBackupFilesDir, FILE_NAME)

    sealed interface LoadResult {
        class Ok(val passphrase: ByteArray, val wasCreated: Boolean) : LoadResult
        /** Kunci butuh autentikasi pengguna terlebih dulu. */
        data object NeedsAuth : LoadResult
        /** Kunci hilang/invalid (mis. sidik jari baru didaftarkan): data tidak bisa dibuka. */
        data object Invalidated : LoadResult
    }

    fun exists(): Boolean = file.exists()

    /** Apakah passphrase saat ini dibungkus kunci yang butuh autentikasi. */
    fun isAuthWrapped(): Boolean? = if (!file.exists()) null else readRecord().requireAuth

    fun loadOrCreate(requireAuthForNew: Boolean): LoadResult {
        if (!file.exists()) {
            val pass = ByteArray(32).also { SecureRandom().nextBytes(it) }
            return try {
                write(pass, requireAuthForNew)
                LoadResult.Ok(pass, wasCreated = true)
            } catch (_: UserNotAuthenticatedException) {
                LoadResult.NeedsAuth
            }
        }
        val rec = try { readRecord() } catch (_: Exception) { return LoadResult.Invalidated }
        return try {
            LoadResult.Ok(keystore.decrypt(rec.iv, rec.ct, rec.requireAuth), wasCreated = false)
        } catch (_: UserNotAuthenticatedException) {
            LoadResult.NeedsAuth
        } catch (_: KeyPermanentlyInvalidatedException) {
            LoadResult.Invalidated
        } catch (_: KeystoreManager.KeyMissingException) {
            LoadResult.Invalidated
        } catch (_: javax.crypto.AEADBadTagException) {
            LoadResult.Invalidated
        }
    }

    /** Bungkus ulang passphrase dengan kunci lain (saat setelan sidik jari diubah). Butuh autentikasi baru bila salah satu kunci auth. */
    fun rewrap(passphrase: ByteArray, requireAuth: Boolean) {
        write(passphrase, requireAuth)
        keystore.deleteKey(!requireAuth)
    }

    fun destroy() {
        file.delete()
        keystore.deleteAll()
    }

    private fun write(pass: ByteArray, requireAuth: Boolean) {
        val (iv, ct) = keystore.encrypt(pass, requireAuth)
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        DataOutputStream(tmp.outputStream().buffered()).use { out ->
            out.writeByte(VERSION)
            out.writeBoolean(requireAuth)
            out.writeInt(iv.size); out.write(iv)
            out.writeInt(ct.size); out.write(ct)
        }
        if (!tmp.renameTo(file)) {
            file.delete(); tmp.renameTo(file)
        }
    }

    private class Record(val requireAuth: Boolean, val iv: ByteArray, val ct: ByteArray)

    private fun readRecord(): Record = DataInputStream(file.inputStream().buffered()).use { inp ->
        val v = inp.readByte().toInt()
        require(v == VERSION)
        val auth = inp.readBoolean()
        val iv = ByteArray(inp.readInt()).also { inp.readFully(it) }
        val ct = ByteArray(inp.readInt()).also { inp.readFully(it) }
        Record(auth, iv, ct)
    }

    companion object {
        private const val FILE_NAME = "db_key.bin"
        private const val VERSION = 1
    }
}
