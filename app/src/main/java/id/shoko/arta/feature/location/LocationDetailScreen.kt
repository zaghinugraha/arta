package id.shoko.arta.feature.location

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.shoko.arta.R
import id.shoko.arta.core.money.MoneyFormat
import id.shoko.arta.core.money.sumOfDec
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.ArtaIcons
import id.shoko.arta.core.ui.AssetClassRes
import id.shoko.arta.core.ui.CircleIconButton
import id.shoko.arta.core.ui.Hairline
import id.shoko.arta.core.ui.InitialAvatar
import id.shoko.arta.core.ui.MutedText
import id.shoko.arta.core.ui.OutlinedCard
import id.shoko.arta.core.ui.PrimaryButton
import id.shoko.arta.core.ui.SecondaryButton
import id.shoko.arta.core.ui.changeColor
import id.shoko.arta.core.ui.money
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.LocationType
import id.shoko.arta.feature.AppViewModel
import id.shoko.arta.feature.addposition.AddPositionRequest
import id.shoko.arta.feature.shell.ChoiceDialog
import id.shoko.arta.feature.shell.ConfirmDialog
import id.shoko.arta.feature.shell.DetailScaffold
import id.shoko.arta.feature.shell.InputDialog
import id.shoko.arta.feature.shell.LocalAppActions
import id.shoko.arta.feature.summary.PositionListRow
import id.shoko.arta.feature.summary.baseCurrency
import java.math.BigDecimal

@Composable
fun LocationDetailScreen(appVm: AppViewModel, locationId: String) {
    val state by appVm.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val c = Arta.colors
    val st = state ?: return
    val location = st.data.locations.firstOrNull { it.id == locationId }
    if (location == null) {
        DetailScaffold(title = "", onBack = actions::back) { item { MutedText(stringResource(R.string.location_not_found), modifier = Modifier.padding(16.dp)) } }
        return
    }
    val v = st.valuation
    val group = v.byLocation.firstOrNull { it.key.id == locationId }
    val positions = group?.positions.orEmpty()
    val cur = baseCurrency()
    val limit = st.settings.concentrationLimit
    val share = group?.sharePct ?: BigDecimal.ZERO
    val over = share > limit
    val dominant = positions.groupBy { it.instrument.assetClass }.maxByOrNull { (_, l) -> l.sumOfDec { it.valueBase } }?.key ?: AssetClass.OTHER
    val nonCash = positions.filter { it.instrument.assetClass != AssetClass.CASH }
    val withHistory = nonCash.filter { it.unrealizedPlBase != null }
    val pl = withHistory.sumOfDec { it.unrealizedPlBase!! }

    var menu by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }

    DetailScaffold(
        title = stringResource(R.string.location),
        onBack = actions::back,
        actions = {
            Box {
                CircleIconButton(ArtaIcons.more, stringResource(R.string.location_options), onClick = { menu = true }, size = 48.dp)
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = c.bg) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.edit_location)) }, onClick = { menu = false; dialog = "edit" })
                    DropdownMenuItem(text = { Text(stringResource(R.string.change_type)) }, onClick = { menu = false; dialog = "type" })
                    DropdownMenuItem(
                        text = { Text(stringResource(if (location.archived) R.string.unarchive else R.string.archive)) },
                        onClick = { menu = false; dialog = "archive" },
                    )
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete_location), color = c.down) }, onClick = { menu = false; dialog = "delete" })
                }
            }
        },
    ) {
        item("head") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    InitialAvatar(location.name, dominant, 48.dp)
                    Column {
                        Text(location.name, style = Arta.type.pageTitle.copy(fontSize = Arta.type.headerTitle.fontSize * 1.1f), color = c.on)
                        MutedText(
                            stringResource(AssetClassRes.locationType(location.type)) + " · " +
                                pluralPositions(positions.size) + if (location.archived) " · " + stringResource(R.string.archived) else "",
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(money(group?.valueBase ?: BigDecimal.ZERO, cur), style = Arta.type.bigNumber.copy(fontSize = Arta.type.bigNumber.fontSize * 0.82f), color = c.on)
                    if (group != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            money(group.changeTodayBase, cur, signed = true) + " (" + MoneyFormat.signedPct(group.changeTodayPct, 2) + ")",
                            style = Arta.type.labelStrong, color = changeColor(group.changeTodayBase),
                        )
                        Text(stringResource(R.string.today), style = Arta.type.label, color = c.muted)
                    }
                }
                // Porsi dari total + penanda batas konsentrasi.
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surf).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.share_of_total), style = Arta.type.caption, color = c.muted)
                        Text(MoneyFormat.pct(share), style = Arta.type.rowValue, color = c.on)
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth().height(16.dp), contentAlignment = Alignment.CenterStart) {
                        val w = maxWidth
                        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.track)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth((share.toFloat() / 100f).coerceIn(0f, 1f)).clip(RoundedCornerShape(4.dp)).background(if (over) c.down else c.accent))
                        }
                        Box(Modifier.offset(x = w * (limit.toFloat() / 100f) - 1.dp).width(2.dp).height(16.dp).background(c.on))
                    }
                    if (over) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(ArtaIcons.warning, null, Modifier.size(14.dp), tint = c.down)
                        Text(stringResource(R.string.over_limit_long, MoneyFormat.pct(limit)), style = Arta.type.small, color = c.down)
                    } else {
                        Text(stringResource(R.string.limit_marker, MoneyFormat.pct(limit)), style = Arta.type.small, color = c.muted)
                    }
                }
                if (nonCash.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedCard(Modifier.weight(1f)) {
                        Column {
                            MutedText(stringResource(R.string.unrealized_pl), style = Arta.type.small)
                            Text(
                                if (withHistory.isEmpty()) "—" else money(pl, cur, compact = true, signed = true),
                                style = Arta.type.rowValue, color = if (withHistory.isEmpty()) c.muted else changeColor(pl),
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    OutlinedCard(Modifier.weight(1f)) {
                        Column {
                            MutedText(stringResource(R.string.counted_from), style = Arta.type.small)
                            Text(stringResource(R.string.x_of_y_positions, withHistory.size, nonCash.size), style = Arta.type.rowValue, color = c.on, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(stringResource(R.string.add_position), onClick = { actions.openAddPosition(AddPositionRequest(locationId = locationId)) }, modifier = Modifier.weight(1f))
                    SecondaryButton(stringResource(R.string.record_trade), onClick = {
                        positions.firstOrNull { it.instrument.assetClass != AssetClass.CASH }?.let { actions.openPosition(it.position.id) }
                            ?: appVm.showMessage("Pilih posisi untuk mencatat beli/jual")
                    }, modifier = Modifier.weight(1f))
                }
            }
            Hairline()
        }
        if (positions.isEmpty()) item("none") { MutedText(stringResource(R.string.no_positions_here), Modifier.padding(16.dp), style = Arta.type.body) }
        items(positions, key = { it.position.id }) { pv -> PositionListRow(pv, showLocation = false) }
    }

    when (dialog) {
        "edit" -> InputDialog(
            title = stringResource(R.string.edit_location),
            fields = listOf(stringResource(R.string.name) to location.name, stringResource(R.string.note) to (location.note ?: "")),
            confirm = stringResource(R.string.save),
            onConfirm = { appVm.updateLocation(location.id, it[0], location.type, it[1].ifBlank { null }) },
            onDismiss = { dialog = null },
            validate = { if (it[0].isBlank()) "Nama tidak boleh kosong" else null },
        )
        "type" -> ChoiceDialog(
            title = stringResource(R.string.change_type),
            options = LocationType.entries,
            selected = location.type,
            label = { stringResource(AssetClassRes.locationType(it)) },
            onSelect = { appVm.updateLocation(location.id, location.name, it, location.note) },
            onDismiss = { dialog = null },
        )
        "archive" -> ConfirmDialog(
            title = stringResource(if (location.archived) R.string.unarchive else R.string.archive),
            body = stringResource(if (location.archived) R.string.unarchive_body else R.string.archive_body),
            confirm = stringResource(if (location.archived) R.string.unarchive else R.string.archive),
            onConfirm = { appVm.setLocationArchived(location.id, !location.archived) },
            onDismiss = { dialog = null },
        )
        "delete" -> ConfirmDialog(
            title = stringResource(R.string.delete_location),
            body = stringResource(R.string.delete_location_body),
            confirm = stringResource(R.string.delete),
            danger = true,
            onConfirm = { appVm.deleteLocation(location.id); actions.back() },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun pluralPositions(n: Int): String = stringResource(R.string.n_positions, n)
