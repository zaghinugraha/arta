package id.shoko.arta.data.db

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Membuka database terenkripsi (Room + SQLCipher) hanya setelah passphrase tersedia (setelah unlock).
 */
class DatabaseHolder(private val context: Context) {

    private val _db = MutableStateFlow<ArtaDatabase?>(null)
    val db: StateFlow<ArtaDatabase?> = _db.asStateFlow()

    val isOpen: Boolean get() = _db.value != null

    fun require(): ArtaDatabase = _db.value ?: error("Database belum dibuka")

    @Synchronized
    fun open(passphrase: ByteArray): ArtaDatabase {
        _db.value?.let { return it }
        ensureLibrary()
        val database = Room.databaseBuilder(context, ArtaDatabase::class.java, ArtaDatabase.NAME)
            // SQLCipher menyimpan referensi array ini dan memakainya lagi setiap membuka koneksi baru
            // (koneksi baca WAL). Beri salinan sendiri: pemanggil boleh mengosongkan array aslinya.
            .openHelperFactory(SupportOpenHelperFactory(passphrase.copyOf()))
            .build()
        // Paksa buka sekarang agar passphrase salah langsung terdeteksi.
        database.openHelper.writableDatabase
        _db.value = database
        return database
    }

    /** Hanya untuk uji: pasang database (mis. in-memory tanpa SQLCipher). */
    @androidx.annotation.VisibleForTesting
    fun attachForTest(database: ArtaDatabase) {
        _db.value = database
    }

    @Synchronized
    fun close() {
        _db.value?.close()
        _db.value = null
    }

    /** Hapus file database (untuk "Hapus semua data" atau pemulihan). */
    @Synchronized
    fun deleteFiles() {
        close()
        context.deleteDatabase(ArtaDatabase.NAME)
    }

    companion object {
        @Volatile private var loaded = false
        fun ensureLibrary() {
            if (!loaded) {
                System.loadLibrary("sqlcipher")
                loaded = true
            }
        }
    }
}
