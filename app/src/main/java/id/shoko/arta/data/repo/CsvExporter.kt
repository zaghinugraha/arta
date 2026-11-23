package id.shoko.arta.data.repo

import id.shoko.arta.domain.valuation.PortfolioValuation
import java.io.OutputStream
import java.math.RoundingMode

/**
 * Ekspor CSV (UTF-8, pemisah koma, desimal titik). File TIDAK terenkripsi — pengguna diperingatkan di UI.
 * Satu file berisi dua bagian: posisi dan riwayat transaksi.
 */
object CsvExporter {
    fun write(out: OutputStream, data: PortfolioData, valuation: PortfolioValuation) {
        val w = out.bufferedWriter(Charsets.UTF_8)
        w.write("# Arta export\n")
        w.write("section,location,location_type,asset_class,symbol,name,quantity,unit,units_per_lot,price,price_currency,price_source,value_${valuation.baseCurrency.lowercase()},avg_cost,unrealized_pl\n")
        valuation.positions.forEach { pv ->
            w.write(
                listOf(
                    "position", pv.location.name, pv.location.type.name, pv.instrument.assetClass.code, pv.instrument.symbol, pv.instrument.name,
                    pv.position.quantity.toPlainString(), pv.instrument.unit.name, pv.instrument.unitsPerLot.toPlainString(),
                    pv.price?.price?.toPlainString().orEmpty(), pv.price?.currency ?: pv.instrument.quoteCurrency, pv.price?.source?.name.orEmpty(),
                    pv.valueBase.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    pv.averageCost?.setScale(4, RoundingMode.HALF_UP)?.toPlainString().orEmpty(),
                    pv.unrealizedPlBase?.setScale(2, RoundingMode.HALF_UP)?.toPlainString().orEmpty(),
                ).joinToString(",", transform = ::esc) + "\n"
            )
        }
        w.write("\nsection,location,symbol,type,quantity,price,fee,currency,executed_at_utc,note\n")
        val posById = data.positions.associateBy { it.id }
        val locById = data.locations.associateBy { it.id }
        data.txns.sortedBy { it.executedAt }.forEach { t ->
            val p = posById[t.positionId]
            val inst = p?.let { data.instruments[it.instrumentId] }
            w.write(
                listOf(
                    "txn", p?.let { locById[it.locationId]?.name }.orEmpty(), inst?.symbol.orEmpty(), t.type.name,
                    t.quantity.toPlainString(), t.price?.toPlainString().orEmpty(), t.fee?.toPlainString().orEmpty(), t.currency,
                    java.time.Instant.ofEpochMilli(t.executedAt).toString(), t.note.orEmpty(),
                ).joinToString(",", transform = ::esc) + "\n"
            )
        }
        w.flush()
    }

    private fun esc(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
}
