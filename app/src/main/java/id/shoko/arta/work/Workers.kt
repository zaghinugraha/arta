package id.shoko.arta.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import id.shoko.arta.App
import id.shoko.arta.core.time.WIB
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Update harga periodik. Hanya berjalan bila database bisa dibuka tanpa interaksi
 * (aplikasi sedang terbuka, atau kunci sidik jari dimatikan). Bila terkunci, dilewati dengan sukses.
 */
class PriceRefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as App).container
        if (!c.openDatabaseInBackground()) return Result.success()
        return try {
            val r = c.priceRepository.refresh()
            c.afterPriceRefresh()
            if (r != null && r.updated == 0 && r.failedSources.isNotEmpty() && runAttemptCount < 3) Result.retry() else Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.success()
        }
    }
}

/** Snapshot harian setelah ±16.30 WIB agar penutupan BEI tercakup. */
class DailySnapshotWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as App).container
        if (!c.openDatabaseInBackground()) return Result.success()
        runCatching { c.priceRepository.refresh() }
        c.afterPriceRefresh(forceSnapshot = true)
        return Result.success()
    }
}

object WorkScheduler {
    private const val PRICE_WORK = "arta_price_refresh"
    private const val SNAPSHOT_WORK = "arta_daily_snapshot"

    /** WorkManager minimal 15 menit; interval 5 menit hanya berlaku saat aplikasi terbuka. */
    fun schedulePrices(context: Context, intervalMin: Int) {
        val minutes = intervalMin.coerceAtLeast(15).toLong()
        val req = PeriodicWorkRequestBuilder<PriceRefreshWorker>(minutes, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PRICE_WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun scheduleSnapshot(context: Context) {
        val now = ZonedDateTime.now(WIB)
        var next = now.with(LocalTime.of(16, 45))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMinutes()
        val req = PeriodicWorkRequestBuilder<DailySnapshotWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(SNAPSHOT_WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }
}
