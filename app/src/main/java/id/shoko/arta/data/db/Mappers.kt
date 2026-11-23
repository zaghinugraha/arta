package id.shoko.arta.data.db

import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.FxRate
import id.shoko.arta.domain.model.Instrument
import id.shoko.arta.domain.model.Location
import id.shoko.arta.domain.model.LocationType
import id.shoko.arta.domain.model.Position
import id.shoko.arta.domain.model.PriceInfo
import id.shoko.arta.domain.model.PriceSource
import id.shoko.arta.domain.model.QuantityUnit
import id.shoko.arta.domain.model.Txn
import id.shoko.arta.domain.model.TxnType

private inline fun <reified E : Enum<E>> enumOr(name: String, default: E): E =
    runCatching { enumValueOf<E>(name) }.getOrDefault(default)

fun LocationEntity.toDomain() = Location(id, name, enumOr(type, LocationType.OTHER), defaultCurrency, note, sortOrder, archived)
fun InstrumentEntity.toDomain() = Instrument(
    id, AssetClass.fromCode(assetClass), symbol, name, quoteCurrency,
    enumOr(unit, QuantityUnit.UNIT), unitsPerLot, enumOr(priceSource, PriceSource.MANUAL), sourceRef,
)
fun PositionEntity.toDomain() = Position(id, locationId, instrumentId, quantity, note, archived)
fun TxnEntity.toDomain() = Txn(id, positionId, enumOr(type, TxnType.ADJUST), quantity, price, fee, currency, executedAt, note)
fun PriceLatestEntity.toDomain() = PriceInfo(instrumentId, price, currency, asOf, fetchedAt, enumOr(source, PriceSource.MANUAL), prevClose)
fun FxRateEntity.toDomain() = FxRate(base, quote, rate, asOf, fetchedAt, source)
