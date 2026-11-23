package id.shoko.arta.data.repo

import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.PriceSource
import id.shoko.arta.domain.model.QuantityUnit
import java.math.BigDecimal

/** Draf instrumen untuk alur Tambah posisi (dari katalog atau dibuat manual). */
data class InstrumentDraft(
    val assetClass: AssetClass,
    val symbol: String,
    val name: String,
    val quoteCurrency: String,
    val unit: QuantityUnit,
    val unitsPerLot: BigDecimal,
    val priceSource: PriceSource,
    val sourceRef: String?,
)

/** Daftar instrumen populer untuk pencarian cepat. Selain ini, pengguna bisa membuat instrumen manual. */
object InstrumentCatalog {
    private fun idx(code: String, name: String) =
        InstrumentDraft(AssetClass.STOCK_IDX, code, name, "IDR", QuantityUnit.LOT, BigDecimal(100), PriceSource.YAHOO, "$code.JK")
    private fun coin(id: String, sym: String, name: String) =
        InstrumentDraft(AssetClass.CRYPTO, sym, name, "IDR", QuantityUnit.COIN, BigDecimal.ONE, PriceSource.COINGECKO, id)
    private fun etf(sym: String, name: String) =
        InstrumentDraft(AssetClass.ETF, sym, name, "USD", QuantityUnit.UNIT, BigDecimal.ONE, PriceSource.YAHOO, sym)

    val entries: List<InstrumentDraft> = listOf(
        idx("BBCA", "Bank Central Asia"), idx("BBRI", "Bank Rakyat Indonesia"), idx("BMRI", "Bank Mandiri"),
        idx("BBNI", "Bank Negara Indonesia"), idx("TLKM", "Telkom Indonesia"), idx("ASII", "Astra International"),
        idx("ANTM", "Aneka Tambang"), idx("UNVR", "Unilever Indonesia"), idx("ICBP", "Indofood CBP"),
        idx("INDF", "Indofood Sukses Makmur"), idx("GOTO", "GoTo Gojek Tokopedia"), idx("ADRO", "Alamtri Resources"),
        idx("PGAS", "Perusahaan Gas Negara"), idx("KLBF", "Kalbe Farma"), idx("UNTR", "United Tractors"),
        idx("MDKA", "Merdeka Copper Gold"), idx("CPIN", "Charoen Pokphand"), idx("AMRT", "Sumber Alfaria Trijaya"),
        coin("bitcoin", "BTC", "Bitcoin"), coin("ethereum", "ETH", "Ethereum"), coin("solana", "SOL", "Solana"),
        coin("binancecoin", "BNB", "BNB"), coin("ripple", "XRP", "XRP"), coin("tether", "USDT", "Tether"),
        coin("usd-coin", "USDC", "USD Coin"), coin("cardano", "ADA", "Cardano"), coin("dogecoin", "DOGE", "Dogecoin"),
        etf("VOO", "Vanguard S&P 500 ETF"), etf("QQQ", "Invesco QQQ Trust"), etf("VTI", "Vanguard Total Stock Market ETF"),
        etf("SPY", "SPDR S&P 500 ETF"), etf("VT", "Vanguard Total World Stock ETF"), etf("SCHD", "Schwab US Dividend Equity ETF"),
        InstrumentDraft(AssetClass.ETF, "XIIT", "Indo Premier IDX30 ETF", "IDR", QuantityUnit.UNIT, BigDecimal.ONE, PriceSource.YAHOO, "XIIT.JK"),
        gold("ANTAM", "Emas Antam"), gold("UBS", "Emas UBS"), gold("PEGADAIAN", "Tabungan Emas Pegadaian"),
        gold("DIGITAL", "Emas digital"),
        cash("IDR", "Rupiah"), cash("USD", "Dolar AS"), cash("SGD", "Dolar Singapura"),
    )

    fun gold(sym: String, name: String) =
        InstrumentDraft(AssetClass.GOLD, sym, name, "IDR", QuantityUnit.GRAM, BigDecimal.ONE, PriceSource.MANUAL, null)

    fun cash(currency: String, name: String) =
        InstrumentDraft(AssetClass.CASH, currency, name, currency, QuantityUnit.CURRENCY, BigDecimal.ONE, PriceSource.NONE, null)

    fun search(assetClass: AssetClass, query: String): List<InstrumentDraft> {
        val q = query.trim().lowercase()
        return entries.filter { it.assetClass == assetClass }
            .filter { q.isEmpty() || it.symbol.lowercase().contains(q) || it.name.lowercase().contains(q) || (it.sourceRef?.lowercase()?.contains(q) == true) }
    }

    /** Draf manual untuk simbol yang tidak ada di katalog. */
    fun manual(assetClass: AssetClass, symbol: String, name: String, quoteCurrency: String): InstrumentDraft {
        val sym = symbol.trim()
        return when (assetClass) {
            AssetClass.STOCK_IDX -> idx(sym.uppercase(), name.ifBlank { sym.uppercase() })
            AssetClass.CRYPTO -> InstrumentDraft(AssetClass.CRYPTO, sym.uppercase(), name.ifBlank { sym }, quoteCurrency, QuantityUnit.COIN, BigDecimal.ONE, PriceSource.COINGECKO, sym.lowercase())
            AssetClass.ETF -> InstrumentDraft(AssetClass.ETF, sym.uppercase(), name.ifBlank { sym.uppercase() }, quoteCurrency, QuantityUnit.UNIT, BigDecimal.ONE, PriceSource.YAHOO, sym.uppercase())
            AssetClass.GOLD -> gold(sym.uppercase(), name.ifBlank { sym })
            AssetClass.CASH -> cash(sym.uppercase(), name.ifBlank { sym.uppercase() })
            AssetClass.MUTUAL_FUND -> InstrumentDraft(AssetClass.MUTUAL_FUND, sym, name.ifBlank { sym }, quoteCurrency, QuantityUnit.UNIT, BigDecimal.ONE, PriceSource.MANUAL, null)
            AssetClass.OTHER -> InstrumentDraft(AssetClass.OTHER, sym, name.ifBlank { sym }, quoteCurrency, QuantityUnit.UNIT, BigDecimal.ONE, PriceSource.MANUAL, null)
        }
    }
}
