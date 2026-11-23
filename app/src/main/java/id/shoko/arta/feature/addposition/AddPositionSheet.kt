package id.shoko.arta.feature.addposition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.shoko.arta.R
import id.shoko.arta.core.money.Dec
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.ArtaIcons
import id.shoko.arta.core.ui.ArtaSwitch
import id.shoko.arta.core.ui.AssetClassRes
import id.shoko.arta.core.ui.AssetTile
import id.shoko.arta.core.ui.CircleIconButton
import id.shoko.arta.core.ui.Hairline
import id.shoko.arta.core.ui.MutedText
import id.shoko.arta.core.ui.Pill
import id.shoko.arta.core.ui.PrimaryButton
import id.shoko.arta.data.repo.InstrumentCatalog
import id.shoko.arta.data.repo.InstrumentDraft
import id.shoko.arta.data.repo.PortfolioRepository
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.LocationType
import id.shoko.arta.domain.model.PriceSource
import id.shoko.arta.feature.AppViewModel
import id.shoko.arta.feature.shell.ArtaTextField
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

data class AddPositionRequest(val locationId: String? = null, val assetClass: AssetClass? = null)

private enum class Step { CLASS, INSTRUMENT, LOCATION, AMOUNT }

@Composable
fun AddPositionSheet(appVm: AppViewModel, request: AddPositionRequest, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val state by appVm.state.collectAsStateWithLifecycle()
    val c = Arta.colors

    var assetClass by remember { mutableStateOf(request.assetClass) }
    var draft by remember { mutableStateOf<InstrumentDraft?>(null) }
    var locationId by remember { mutableStateOf(request.locationId) }
    var step by remember { mutableStateOf(if (request.assetClass == null) Step.CLASS else Step.INSTRUMENT) }

    fun close() { scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() } }
    fun next() {
        step = when (step) {
            Step.CLASS -> Step.INSTRUMENT
            Step.INSTRUMENT -> if (locationId == null) Step.LOCATION else Step.AMOUNT
            Step.LOCATION -> Step.AMOUNT
            Step.AMOUNT -> Step.AMOUNT
        }
    }
    fun back() {
        step = when (step) {
            Step.CLASS -> { close(); Step.CLASS }
            Step.INSTRUMENT -> if (request.assetClass == null) Step.CLASS else { close(); Step.INSTRUMENT }
            Step.LOCATION -> Step.INSTRUMENT
            Step.AMOUNT -> if (request.locationId == null) Step.LOCATION else Step.INSTRUMENT
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = c.bg,
        dragHandle = { Box(Modifier.padding(top = 10.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(c.hairStrong)) },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(if (step == Step.CLASS) ArtaIcons.close else ArtaIcons.back, stringResource(R.string.back), onClick = ::back, size = 48.dp)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.add_position), style = Arta.type.headerTitle, color = c.on)
                    MutedText(
                        stringResource(
                            when (step) {
                                Step.CLASS -> R.string.step_class
                                Step.INSTRUMENT -> R.string.step_instrument
                                Step.LOCATION -> R.string.step_location
                                Step.AMOUNT -> R.string.step_amount
                            }
                        ),
                        style = Arta.type.small,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            when (step) {
                Step.CLASS -> ClassGrid { assetClass = it; draft = null; next() }
                Step.INSTRUMENT -> InstrumentStep(assetClass ?: AssetClass.OTHER) { draft = it; next() }
                Step.LOCATION -> LocationStep(
                    locations = state?.data?.locations.orEmpty().filter { !it.archived }.map { it.id to it.name },
                    onPick = { locationId = it; next() },
                    onCreate = { name, type -> scope.launch { locationId = appVm.addLocationNow(name, type); next() } },
                )
                Step.AMOUNT -> AmountStep(
                    draft = draft!!,
                    locationName = state?.data?.locations?.firstOrNull { it.id == locationId }?.name.orEmpty(),
                    onSave = { qty, buy, manualPrice ->
                        appVm.addPosition(locationId!!, draft!!, qty, buy, manualPrice) { close() }
                    },
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ClassGrid(onPick: (AssetClass) -> Unit) {
    val c = Arta.colors
    val classes = AssetClass.displayOrder
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        classes.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { cls ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).border(1.dp, c.hair, RoundedCornerShape(18.dp))
                            .clickable(role = Role.Button) { onPick(cls) }.padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AssetTile(cls)
                        Text(stringResource(AssetClassRes.label(cls)), style = Arta.type.captionStrong, color = c.on, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun InstrumentStep(cls: AssetClass, onPick: (InstrumentDraft) -> Unit) {
    val c = Arta.colors
    var query by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(if (cls == AssetClass.ETF) "USD" else "IDR") }
    val results = remember(cls, query) { InstrumentCatalog.search(cls, query) }
    Column(Modifier.padding(horizontal = 16.dp).heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ArtaTextField(query, { query = it }, stringResource(R.string.search_instrument_hint, stringResource(AssetClassRes.label(cls))))
        results.take(12).forEach { d ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp)).clickable { onPick(d) }.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AssetTile(cls, 40.dp, 30.dp, 12.dp)
                Column(Modifier.weight(1f)) {
                    Text(d.symbol, style = Arta.type.rowName, color = c.on)
                    MutedText(d.name + " · " + d.quoteCurrency, style = Arta.type.small, maxLines = 1)
                }
                Icon(ArtaIcons.chevronRight, null, Modifier.size(18.dp), tint = c.muted)
            }
        }
        Hairline()
        Text(stringResource(R.string.create_manual), style = Arta.type.labelStrong, color = c.accent, modifier = Modifier.padding(top = 4.dp))
        MutedText(stringResource(manualHint(cls)), style = Arta.type.small)
        ArtaTextField(name, { name = it }, stringResource(R.string.instrument_name_optional))
        if (cls != AssetClass.STOCK_IDX && cls != AssetClass.CASH) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MutedText(stringResource(R.string.currency), style = Arta.type.caption)
                listOf("IDR", "USD").forEach { cur -> Pill(cur, currency == cur, onClick = { currency = cur }) }
            }
        }
        PrimaryButton(
            stringResource(R.string.use_symbol, query.trim().ifEmpty { "…" }),
            enabled = query.isNotBlank(),
            onClick = { onPick(InstrumentCatalog.manual(cls, query, name, if (cls == AssetClass.CASH) query.trim().uppercase() else currency)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun manualHint(cls: AssetClass): Int = when (cls) {
    AssetClass.STOCK_IDX -> R.string.manual_hint_stock
    AssetClass.CRYPTO -> R.string.manual_hint_crypto
    AssetClass.ETF -> R.string.manual_hint_etf
    AssetClass.CASH -> R.string.manual_hint_cash
    else -> R.string.manual_hint_other
}

@Composable
private fun LocationStep(locations: List<Pair<String, String>>, onPick: (String) -> Unit, onCreate: (String, LocationType) -> Unit) {
    val c = Arta.colors
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(LocationType.BROKER) }
    Column(Modifier.padding(horizontal = 16.dp).heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        locations.forEach { (id, n) ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).clickable { onPick(id) }.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(n, style = Arta.type.rowName, color = c.on, modifier = Modifier.weight(1f))
                Icon(ArtaIcons.chevronRight, null, Modifier.size(18.dp), tint = c.muted)
            }
        }
        if (locations.isNotEmpty()) Hairline()
        Text(stringResource(R.string.new_location), style = Arta.type.labelStrong, color = c.accent, modifier = Modifier.padding(top = 4.dp))
        ArtaTextField(name, { name = it }, stringResource(R.string.location_name_hint))
        LocationTypePicker(type) { type = it }
        PrimaryButton(stringResource(R.string.create_location), enabled = name.isNotBlank(), onClick = { onCreate(name, type) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun LocationTypePicker(selected: LocationType, onSelect: (LocationType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LocationType.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { t -> Pill(stringResource(AssetClassRes.locationType(t)), t == selected, onClick = { onSelect(t) }, minWidth = 0.dp) }
            }
        }
    }
}

@Composable
private fun AmountStep(
    draft: InstrumentDraft,
    locationName: String,
    onSave: (BigDecimal, PortfolioRepository.BuyInput?, BigDecimal?) -> Unit,
) {
    val c = Arta.colors
    val isCash = draft.assetClass == AssetClass.CASH
    val needsManualPrice = draft.priceSource == PriceSource.MANUAL
    var qty by remember { mutableStateOf("") }
    var manualPrice by remember { mutableStateOf("") }
    var withBuy by remember { mutableStateOf(false) }
    var buyPrice by remember { mutableStateOf("") }
    var buyDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var fee by remember { mutableStateOf("") }
    val unit = stringResource(AssetClassRes.unit(draft.unit))
    val q = Dec.parseUser(qty)
    val mp = Dec.parseUser(manualPrice)
    val bp = Dec.parseUser(buyPrice)
    val date = runCatching { LocalDate.parse(buyDate.trim()) }.getOrNull()
    val valid = q != null && q.signum() > 0 &&
        (!needsManualPrice || (mp != null && mp.signum() > 0)) &&
        (!withBuy || (bp != null && bp.signum() > 0 && date != null))

    Column(Modifier.padding(horizontal = 16.dp).heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AssetTile(draft.assetClass, 44.dp, 34.dp, 14.dp)
            Column {
                Text(draft.symbol + " · " + draft.name, style = Arta.type.rowName, color = c.on, maxLines = 1, overflow = TextOverflow.Ellipsis)
                MutedText(locationName, style = Arta.type.small)
            }
        }
        ArtaTextField(
            qty, { qty = it },
            if (isCash) stringResource(R.string.balance) + " (" + draft.quoteCurrency + ")" else stringResource(R.string.quantity) + " ($unit)",
            numeric = true,
            supporting = if (draft.assetClass == AssetClass.STOCK_IDX) stringResource(R.string.lot_hint) else null,
        )
        if (needsManualPrice) {
            ArtaTextField(
                manualPrice, { manualPrice = it },
                stringResource(R.string.price_per_unit, unit, draft.quoteCurrency),
                numeric = true,
                supporting = stringResource(if (draft.assetClass == AssetClass.GOLD) R.string.gold_buyback_note else R.string.manual_price_note),
            )
        }
        if (!isCash) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.record_buy_price), style = Arta.type.bodyLarge, color = c.on)
                    MutedText(stringResource(R.string.record_buy_price_hint), style = Arta.type.small)
                }
                ArtaSwitch(withBuy, { withBuy = it })
            }
            if (withBuy) {
                ArtaTextField(buyPrice, { buyPrice = it }, stringResource(R.string.price_per_unit_short, draft.quoteCurrency) + if (draft.assetClass == AssetClass.STOCK_IDX) " · " + stringResource(R.string.per_share) else "", numeric = true)
                ArtaTextField(buyDate, { buyDate = it }, stringResource(R.string.date_iso))
                ArtaTextField(fee, { fee = it }, stringResource(R.string.fee_optional), numeric = true)
            }
        }
        PrimaryButton(
            stringResource(R.string.save),
            enabled = valid,
            onClick = {
                val buy = if (withBuy && !isCash) PortfolioRepository.BuyInput(
                    bp!!, fee.takeIf { it.isNotBlank() }?.let { Dec.parseUser(it) },
                    date!!.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                ) else null
                onSave(q!!, buy, if (needsManualPrice) mp else null)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}
