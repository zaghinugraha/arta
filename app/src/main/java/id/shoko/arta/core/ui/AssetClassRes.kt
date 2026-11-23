package id.shoko.arta.core.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import id.shoko.arta.R
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.LocationType
import id.shoko.arta.domain.model.QuantityUnit

object AssetClassRes {
    @StringRes
    fun label(c: AssetClass): Int = when (c) {
        AssetClass.STOCK_IDX -> R.string.class_stock
        AssetClass.ETF -> R.string.class_etf
        AssetClass.MUTUAL_FUND -> R.string.class_mutual_fund
        AssetClass.CRYPTO -> R.string.class_crypto
        AssetClass.GOLD -> R.string.class_gold
        AssetClass.CASH -> R.string.class_cash
        AssetClass.OTHER -> R.string.class_other
    }

    /** Label pendek untuk legenda alokasi. */
    @StringRes
    fun short(c: AssetClass): Int = when (c) {
        AssetClass.STOCK_IDX -> R.string.class_stock_short
        else -> label(c)
    }

    @DrawableRes
    fun illustration(c: AssetClass): Int = when (c) {
        AssetClass.STOCK_IDX -> R.drawable.ic_asset_stock
        AssetClass.ETF -> R.drawable.ic_asset_etf
        AssetClass.MUTUAL_FUND -> R.drawable.ic_asset_mutual_fund
        AssetClass.CRYPTO -> R.drawable.ic_asset_crypto
        AssetClass.GOLD -> R.drawable.ic_asset_gold
        AssetClass.CASH -> R.drawable.ic_asset_savings
        AssetClass.OTHER -> R.drawable.ic_asset_other
    }

    @StringRes
    fun unit(u: QuantityUnit): Int = when (u) {
        QuantityUnit.LOT -> R.string.unit_lot
        QuantityUnit.SHARE -> R.string.unit_share
        QuantityUnit.UNIT -> R.string.unit_unit
        QuantityUnit.COIN -> R.string.unit_coin
        QuantityUnit.GRAM -> R.string.unit_gram
        QuantityUnit.CURRENCY -> R.string.unit_currency
    }

    @StringRes
    fun locationType(t: LocationType): Int = when (t) {
        LocationType.BROKER -> R.string.loc_broker
        LocationType.CRYPTO_EXCHANGE -> R.string.loc_crypto_exchange
        LocationType.BANK -> R.string.loc_bank
        LocationType.PHYSICAL -> R.string.loc_physical
        LocationType.PLATFORM -> R.string.loc_platform
        LocationType.WALLET -> R.string.loc_wallet
        LocationType.OTHER -> R.string.loc_other
    }
}
