package id.shoko.arta.feature.position

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.shoko.arta.R
import id.shoko.arta.core.money.Dec
import id.shoko.arta.core.money.MoneyFormat
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.ArtaIcons
import id.shoko.arta.core.ui.AssetClassRes
import id.shoko.arta.core.ui.AssetTile
import id.shoko.arta.core.ui.CircleIconButton
import id.shoko.arta.core.ui.Hairline
import id.shoko.arta.core.ui.LocalHideAmounts
import id.shoko.arta.core.ui.MutedText
import id.shoko.arta.core.ui.OutlinedCard
import id.shoko.arta.core.ui.PrimaryButton
import id.shoko.arta.core.ui.SecondaryButton
import id.shoko.arta.core.ui.SectionTitle
import id.shoko.arta.core.ui.changeColor
import id.shoko.arta.core.ui.money
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.PriceSource
import id.shoko.arta.domain.model.Txn
import id.shoko.arta.domain.model.TxnType
import id.shoko.arta.domain.valuation.PositionValuation
import id.shoko.arta.feature.AppViewModel
import id.shoko.arta.feature.shell.ConfirmDialog
import id.shoko.arta.feature.shell.DetailScaffold
import id.shoko.arta.feature.shell.InputDialog
import id.shoko.arta.feature.shell.LocalAppActions
import id.shoko.arta.feature.summary.baseCurrency
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"))
private val dateTimeFmt = DateTimeFormatter.ofPattern("d MMM, HH.mm", Locale.forLanguageTag("id-ID"))

fun formatTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateTimeFmt)
fun formatDate(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateFmt)

@Composable
fun quantityLine(pv: PositionValuation): String {
    val i = pv.instrument
    if (i.assetClass == AssetClass.CASH) return stringResource(R.string.balance) + " · " + i.quoteCurrency
    val unit = stringResource(AssetClassRes.unit(i.unit))
    val price = pv.unitPrice?.let { "@" + MoneyFormat.price(it, pv.price?.currency ?: i.quoteCurrency).removePrefix("Rp ") } ?: ""
    return MoneyFormat.quantity(pv.position.quantity) + " " + unit + if (price.isNotEmpty()) " · $price" else ""
}

@Composable
fun sourceLabel(s: PriceSource): String = stringResource(
    when (s) {
        PriceSource.YAHOO -> R.string.src_yahoo
        PriceSource.COINGECKO -> R.string.src_coingecko
        PriceSource.GOLD_SPOT -> R.string.src_gold_spot
        PriceSource.MANUAL -> R.string.src_manual
        PriceSource.NONE -> R.string.src_none
    }
)

@Composable
fun PositionDetailScreen(appVm: AppViewModel, positionId: String) {
    val state by appVm.state.collectAsStateWithLifecycle()
    val txns by remember(positionId) { appVm.observeTxns(positionId) }.collectAsState(initial = emptyList())
    val actions = LocalAppActions.current
    val c = Arta.colors
    val st = state ?: return
    val position = st.data.positions.firstOrNull { it.id == positionId }
    val pv = st.valuation.positions.firstOrNull { it.position.id == positionId }
    var dialog by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var txnToDelete by remember { mutableStateOf<Txn?>(null) }

    if (position == null) {
        DetailScaffold(title = "", onBack = actions::back) { item { MutedText(stringResource(R.string.position_not_found), Modifier.padding(16.dp)) } }
        return
    }
    val instrument = st.data.instruments[position.instrumentId] ?: return
    val location = st.data.locations.firstOrNull { it.id == position.locationId }
    val price = st.data.prices[instrument.id]
    val cur = baseCurrency()
    val isCash = instrument.assetClass == AssetClass.CASH
    val canManualPrice = instrument.priceSource == PriceSource.MANUAL || instrument.assetClass == AssetClass.GOLD || instrument.assetClass == AssetClass.MUTUAL_FUND || instrument.assetClass == AssetClass.OTHER

    DetailScaffold(
        title = instrument.symbol,
        onBack = actions::back,
        actions = {
            Box {
                CircleIconButton(ArtaIcons.more, stringResource(R.string.options), onClick = { menu = true }, size = 48.dp)
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = c.bg) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.change_quantity)) }, onClick = { menu = false; dialog = "qty" })
                    if (canManualPrice) DropdownMenuItem(text = { Text(stringResource(R.string.set_price)) }, onClick = { menu = false; dialog = "price" })
                    DropdownMenuItem(text = { Text(stringResource(if (position.archived) R.string.unarchive else R.string.archive)) }, onClick = { menu = false; appVm.setPositionArchived(position.id, !position.archived) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete_position), color = c.down) }, onClick = { menu = false; dialog = "delete" })
                }
            }
        },
    ) {
        item("head") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssetTile(instrument.assetClass)
                    Column(Modifier.weight(1f)) {
                        Text(instrument.name, style = Arta.type.rowName, color = c.on)
                        MutedText(stringResource(AssetClassRes.label(instrument.assetClass)) + (location?.let { " · " + it.name } ?: ""))
                    }
                }
                Text(
                    if (pv == null) "—" else if (pv.missingPrice) stringResource(R.string.no_price) else money(pv.valueBase, cur),
                    style = Arta.type.bigNumber.copy(fontSize = Arta.type.bigNumber.fontSize * 0.82f), color = c.on,
                )
                if (pv != null && !isCash && pv.price?.previousClose != null) {
                    Text(
                        money(pv.changeTodayBase, cur, signed = true) + " (" + MoneyFormat.signedPct(pv.changeTodayPct, 2) + ") " + stringResource(R.string.today),
                        style = Arta.type.labelStrong, color = changeColor(pv.changeTodayBase),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedCard(Modifier.weight(1f)) {
                        Column {
                            MutedText(stringResource(if (isCash) R.string.balance else R.string.quantity), style = Arta.type.small)
                            Text(
                                if (isCash) money(position.quantity, instrument.quoteCurrency)
                                else MoneyFormat.quantity(position.quantity) + " " + stringResource(AssetClassRes.unit(instrument.unit)),
                                style = Arta.type.rowValue, color = c.on, modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    if (!isCash) OutlinedCard(Modifier.weight(1f)) {
                        Column {
                            MutedText(stringResource(R.string.last_price), style = Arta.type.small)
                            Text(price?.let { MoneyFormat.price(it.price, it.currency) } ?: "—", style = Arta.type.rowValue, color = c.on, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
                if (!isCash) {
                    val stale = pv?.isStale == true
                    Text(
                        if (price == null) stringResource(R.string.price_missing_hint)
                        else stringResource(R.string.price_meta, sourceLabel(price.source), formatTime(price.fetchedAt)) + if (stale) " · " + stringResource(R.string.stale) else "",
                        style = Arta.type.small, color = if (stale || price == null) c.down else c.muted,
                    )
                    if (instrument.assetClass == AssetClass.STOCK_IDX) MutedText(stringResource(R.string.lot_hint), style = Arta.type.small)
                }
                if (!isCash && pv != null) {
                    if (pv.hasHistory && pv.averageCost != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedCard(Modifier.weight(1f)) {
                                Column {
                                    MutedText(stringResource(R.string.avg_price), style = Arta.type.small)
                                    Text(MoneyFormat.price(pv.averageCost, instrument.quoteCurrency), style = Arta.type.rowValue, color = c.on, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                            OutlinedCard(Modifier.weight(1f)) {
                                Column {
                                    MutedText(stringResource(R.string.unrealized_pl), style = Arta.type.small)
                                    val pl = pv.unrealizedPlBase ?: BigDecimal.ZERO
                                    Text(
                                        money(pl, cur, compact = true, signed = true) + " (" + MoneyFormat.signedPct(pv.unrealizedPlPct ?: BigDecimal.ZERO) + ")",
                                        style = Arta.type.rowValue, color = changeColor(pl), modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                        }
                    } else {
                        MutedText(stringResource(R.string.no_buy_history_hint), style = Arta.type.caption)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(stringResource(if (isCash) R.string.record_deposit else R.string.record_trade), onClick = { dialog = "trade" }, modifier = Modifier.weight(1f))
                    SecondaryButton(stringResource(R.string.change_quantity), onClick = { dialog = "qty" }, modifier = Modifier.weight(1f))
                }
                if (canManualPrice) SecondaryButton(stringResource(R.string.set_price), onClick = { dialog = "price" }, modifier = Modifier.fillMaxWidth())
            }
            Hairline()
        }
        item("tx-title") { SectionTitle(stringResource(R.string.txn_history), Modifier.padding(16.dp, 16.dp, 16.dp, 6.dp)) }
        if (txns.isEmpty()) item("tx-empty") { MutedText(stringResource(R.string.no_txn), Modifier.padding(horizontal = 16.dp), style = Arta.type.body) }
        items(txns, key = { it.id }) { t -> TxnRow(t, instrument.quoteCurrency, onLongPress = { txnToDelete = t }) }
    }

    when (dialog) {
        "qty" -> InputDialog(
            title = stringResource(R.string.change_quantity),
            fields = listOf((if (isCash) stringResource(R.string.balance) else stringResource(R.string.quantity) + " (" + stringResource(AssetClassRes.unit(instrument.unit)) + ")") to MoneyFormat.quantity(position.quantity)),
            numeric = listOf(true),
            confirm = stringResource(R.string.save),
            note = stringResource(R.string.change_quantity_note),
            onConfirm = { v -> Dec.parseUser(v[0])?.let { appVm.setQuantity(position.id, it) } },
            onDismiss = { dialog = null },
            validate = { v -> if (Dec.parseUser(v[0])?.let { it.signum() >= 0 } != true) "Jumlah tidak valid" else null },
        )
        "price" -> InputDialog(
            title = stringResource(R.string.set_price),
            fields = listOf(stringResource(R.string.price_per_unit, stringResource(AssetClassRes.unit(instrument.unit)), instrument.quoteCurrency) to (price?.price?.let { MoneyFormat.quantity(it, 4) } ?: "")),
            numeric = listOf(true),
            confirm = stringResource(R.string.save),
            note = if (instrument.assetClass == AssetClass.GOLD) stringResource(R.string.gold_buyback_note) else stringResource(R.string.manual_price_note),
            onConfirm = { v -> Dec.parseUser(v[0])?.let { appVm.setManualPrice(instrument.id, it, instrument.quoteCurrency) } },
            onDismiss = { dialog = null },
            validate = { v -> if (Dec.parseUser(v[0])?.let { it.signum() > 0 } != true) "Harga tidak valid" else null },
        )
        "trade" -> TradeDialog(
            isCash = isCash,
            unitLabel = stringResource(AssetClassRes.unit(instrument.unit)),
            currency = instrument.quoteCurrency,
            onConfirm = { type, qty, p, fee, at -> appVm.recordTrade(position.id, type, qty, p, fee, at, null) },
            onDismiss = { dialog = null },
        )
        "delete" -> ConfirmDialog(
            title = stringResource(R.string.delete_position),
            body = stringResource(R.string.delete_position_body),
            confirm = stringResource(R.string.delete),
            danger = true,
            onConfirm = { appVm.deletePosition(position.id); actions.back() },
            onDismiss = { dialog = null },
        )
    }
    txnToDelete?.let { t ->
        ConfirmDialog(
            title = stringResource(R.string.delete_txn),
            body = stringResource(R.string.delete_txn_body),
            confirm = stringResource(R.string.delete),
            danger = true,
            onConfirm = { appVm.deleteTxn(t.id) },
            onDismiss = { txnToDelete = null },
        )
    }
}

@Composable
private fun TxnRow(t: Txn, currency: String, onLongPress: () -> Unit) {
    val c = Arta.colors
    val hidden = LocalHideAmounts.current
    val label = stringResource(
        when (t.type) {
            TxnType.BUY -> R.string.txn_buy; TxnType.SELL -> R.string.txn_sell; TxnType.DEPOSIT -> R.string.txn_deposit
            TxnType.WITHDRAW -> R.string.txn_withdraw; TxnType.DIVIDEND -> R.string.txn_dividend; TxnType.ADJUST -> R.string.txn_adjust
        }
    )
    Column(Modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.delete_txn), onClick = onLongPress)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = Arta.type.rowName.copy(fontSize = Arta.type.body.fontSize), color = c.on)
                MutedText(formatDate(t.executedAt) + (t.note?.let { " · $it" } ?: ""), style = Arta.type.small)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text((if (t.quantity.signum() > 0 && t.type == TxnType.ADJUST) "+" else "") + MoneyFormat.quantity(t.quantity), style = Arta.type.rowValue, color = c.on)
                t.price?.let { MutedText(if (hidden) "@ ••••" else "@" + MoneyFormat.price(it, currency), style = Arta.type.small) }
            }
        }
        Hairline(Modifier.padding(start = 16.dp))
    }
}

@Composable
fun TradeDialog(
    isCash: Boolean,
    unitLabel: String,
    currency: String,
    onConfirm: (TxnType, BigDecimal, BigDecimal?, BigDecimal?, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var type by remember { mutableStateOf(if (isCash) TxnType.DEPOSIT else TxnType.BUY) }
    val types = if (isCash) listOf(TxnType.DEPOSIT, TxnType.WITHDRAW) else listOf(TxnType.BUY, TxnType.SELL)
    val title = stringResource(if (isCash) R.string.record_deposit else R.string.record_trade)
    val labels = mapOf(
        TxnType.BUY to stringResource(R.string.txn_buy), TxnType.SELL to stringResource(R.string.txn_sell),
        TxnType.DEPOSIT to stringResource(R.string.txn_deposit), TxnType.WITHDRAW to stringResource(R.string.txn_withdraw),
    )
    val today = LocalDate.now().toString()
    val qtyLabel = if (isCash) stringResource(R.string.amount) + " ($currency)" else stringResource(R.string.quantity) + " ($unitLabel)"
    val fields = buildList {
        add(qtyLabel to "")
        if (!isCash) {
            add(stringResource(R.string.price_per_unit_short, currency) to "")
            add(stringResource(R.string.fee_optional) to "")
        }
        add(stringResource(R.string.date_iso) to today)
    }
    Column {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Arta.colors.bg,
            title = { Text(title, style = Arta.type.section) },
            text = {
                var values by remember { mutableStateOf(fields.map { it.second }) }
                val qty = Dec.parseUser(values[0])
                val price = if (isCash) null else Dec.parseUser(values[1])
                val fee = if (isCash) null else values[2].takeIf { it.isNotBlank() }?.let { Dec.parseUser(it) }
                val date = runCatching { LocalDate.parse(values.last().trim()) }.getOrNull()
                val valid = qty != null && qty.signum() > 0 && (isCash || (price != null && price.signum() > 0)) && date != null
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.forEach { t -> id.shoko.arta.core.ui.Pill(labels.getValue(t), t == type, onClick = { type = t }) }
                    }
                    fields.forEachIndexed { i, (label, _) ->
                        id.shoko.arta.feature.shell.ArtaTextField(values[i], { nv -> values = values.toMutableList().also { it[i] = nv } }, label, numeric = i < fields.lastIndex)
                    }
                    if (!isCash && type == TxnType.BUY) MutedText(stringResource(R.string.trade_note_buy), style = Arta.type.small)
                    androidx.compose.material3.TextButton(
                        enabled = valid,
                        onClick = {
                            val at = date!!.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            onConfirm(type, qty!!, price, fee, at); onDismiss()
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text(stringResource(R.string.save), color = if (valid) Arta.colors.accent else Arta.colors.muted, style = Arta.type.labelStrong) }
                }
            },
            confirmButton = {},
            dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Arta.colors.muted) } },
        )
    }
}
