package id.shoko.arta.data.prices

import id.shoko.arta.domain.model.Instrument
import id.shoko.arta.domain.model.PriceSource
import java.math.BigDecimal
import java.time.Instant

data class PriceQuote(
    val instrumentId: String,
    val price: BigDecimal,
    val currency: String,
    val asOf: Instant,
    val previousClose: BigDecimal? = null,
)

/** Semua provider dapat dimatikan/diganti tanpa menyentuh UI. */
interface PriceProvider {
    val source: PriceSource
    /** Batch. Instrumen yang gagal diambil cukup tidak dikembalikan (harga lama tetap dipakai & ditandai basi). */
    suspend fun fetch(instruments: List<Instrument>): List<PriceQuote>
}

data class FxQuote(val base: String, val quote: String, val rate: BigDecimal, val asOf: Instant, val source: String)

interface FxProvider {
    suspend fun fetch(base: String, quotes: Set<String>): List<FxQuote>
}
