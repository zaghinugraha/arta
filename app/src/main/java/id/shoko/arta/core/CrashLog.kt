package id.shoko.arta.core

import android.content.Context
import android.os.Build
import id.shoko.arta.BuildConfig
import java.io.File

/**
 * Pencatat crash LOKAL (tanpa jaringan, tanpa SDK pihak ketiga).
 * Jejak error disimpan di file privat dan ditampilkan saat aplikasi dibuka berikutnya
 * agar pengguna bisa menyalinnya. Tidak berisi nominal/nama lokasi — hanya stack trace.
 */
object CrashLog {
    private const val FILE = "last_crash.txt"

    fun install(context: Context) {
        val file = File(context.noBackupFilesDir, FILE)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                file.writeText(
                    "Arta ${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE}) · ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                        "Thread: ${thread.name}\n\n" + error.stackTraceToString().take(12_000)
                )
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? = File(context.noBackupFilesDir, FILE).takeIf { it.exists() }?.readText()
    fun clear(context: Context) { File(context.noBackupFilesDir, FILE).delete() }
}
