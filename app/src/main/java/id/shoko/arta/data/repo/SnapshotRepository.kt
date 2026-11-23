package id.shoko.arta.data.repo

import id.shoko.arta.core.time.Clock
import id.shoko.arta.data.db.DatabaseHolder
import id.shoko.arta.data.db.SnapshotDailyEntity
import id.shoko.arta.data.db.SnapshotItemEntity
import id.shoko.arta.domain.valuation.PortfolioValuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate

data class SnapshotPoint(val date: LocalDate, val total: BigDecimal)

/** Snapshot harian: dasar grafik 1B/3B/1T/Semua dan statistik Bulan ini/Tahun ini. */
@OptIn(ExperimentalCoroutinesApi::class)
class SnapshotRepository(private val holder: DatabaseHolder, private val clock: Clock) {

    val points: Flow<List<SnapshotPoint>> = holder.db.filterNotNull().flatMapLatest { it.snapshotDao().observeDaily() }
        .map { l -> l.map { SnapshotPoint(LocalDate.parse(it.date), it.totalBase) } }

    /** Tulis/perbarui snapshot hari ini dari valuasi saat ini. */
    suspend fun writeToday(v: PortfolioValuation) {
        val db = holder.db.value ?: return
        if (v.positions.isEmpty()) return
        val date = clock.today().toString()
        db.snapshotDao().replaceSnapshot(
            SnapshotDailyEntity(date, v.total, v.baseCurrency, clock.nowMillis()),
            v.byClass.map { SnapshotItemEntity(date, it.key.code, it.valueBase) },
        )
    }

    suspend fun hasToday(): Boolean = holder.db.value?.snapshotDao()?.getDaily(clock.today().toString()) != null
}
