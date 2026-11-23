package id.shoko.arta.domain.model

enum class AssetClass(val code: String) {
    STOCK_IDX("STOCK_IDX"),
    ETF("ETF"),
    MUTUAL_FUND("MUTUAL_FUND"),
    CRYPTO("CRYPTO"),
    GOLD("GOLD"),
    CASH("CASH"),
    OTHER("OTHER");

    companion object {
        /** Urutan tampil default (sesuai mockup). */
        val displayOrder = listOf(STOCK_IDX, GOLD, CRYPTO, CASH, ETF, MUTUAL_FUND, OTHER)
        fun fromCode(code: String): AssetClass = entries.firstOrNull { it.code == code } ?: OTHER
    }
}

enum class LocationType { BROKER, CRYPTO_EXCHANGE, BANK, PHYSICAL, PLATFORM, WALLET, OTHER }

enum class QuantityUnit { LOT, SHARE, UNIT, COIN, GRAM, CURRENCY }

enum class PriceSource { YAHOO, COINGECKO, GOLD_SPOT, MANUAL, NONE }

enum class TxnType { BUY, SELL, DEPOSIT, WITHDRAW, DIVIDEND, ADJUST }

enum class GoldReference { BUYBACK_MANUAL, SPOT }

enum class GlassMode { AUTO, FULL, LIGHT, OFF }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ChatRole { USER, ASSISTANT }
