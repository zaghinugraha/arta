package id.shoko.arta.feature.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.shoko.arta.R
import id.shoko.arta.core.money.MoneyFormat
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.AssetClassRes
import id.shoko.arta.core.ui.AssetTile
import id.shoko.arta.core.ui.Hairline
import id.shoko.arta.core.ui.ListRow
import id.shoko.arta.core.ui.MutedText
import id.shoko.arta.core.ui.PrimaryButton
import id.shoko.arta.core.ui.ValueColumn
import id.shoko.arta.core.ui.changeColor
import id.shoko.arta.core.ui.money
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.valuation.PositionValuation
import id.shoko.arta.feature.AppViewModel
import id.shoko.arta.feature.addposition.AddPositionRequest
import id.shoko.arta.feature.shell.DetailScaffold
import id.shoko.arta.feature.shell.LocalAppActions
import id.shoko.arta.feature.position.quantityLine

/** Daftar posisi dalam satu kelas aset, lintas lokasi. */
@Composable
fun AssetClassDetailScreen(appVm: AppViewModel, assetClass: AssetClass) {
    val state by appVm.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val c = Arta.colors
    val v = state?.valuation
    val group = v?.byClass?.firstOrNull { it.key == assetClass }
    DetailScaffold(
        title = stringResource(AssetClassRes.label(assetClass)),
        onBack = actions::back,
        bottomBar = { PrimaryButton(stringResource(R.string.add_position), onClick = { actions.openAddPosition(AddPositionRequest(assetClass = assetClass)) }, modifier = Modifier) },
    ) {
        item("head") {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                AssetTile(assetClass, 64.dp, 50.dp, 20.dp)
                Column {
                    Text(money(group?.valueBase ?: java.math.BigDecimal.ZERO, baseCurrency()), style = Arta.type.headerTitle, color = c.on)
                    if (group != null) {
                        Text(
                            MoneyFormat.pct(group.sharePct) + " " + stringResource(R.string.of_total) + " · " + MoneyFormat.signedPct(group.changeTodayPct) + " " + stringResource(R.string.today),
                            style = Arta.type.caption, color = c.muted,
                        )
                    }
                }
            }
            Hairline()
        }
        val positions = group?.positions.orEmpty()
        if (positions.isEmpty()) {
            item("none") { MutedText(stringResource(R.string.no_positions_in_class), modifier = Modifier.padding(16.dp), style = Arta.type.body) }
        }
        items(positions, key = { it.position.id }) { pv -> PositionListRow(pv, showLocation = true) }
    }
}

@Composable
fun PositionListRow(pv: PositionValuation, showLocation: Boolean) {
    val c = Arta.colors
    val actions = LocalAppActions.current
    val cur = baseCurrency()
    ListRow(onClick = { actions.openPosition(pv.position.id) }, minHeight = 72.dp) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(pv.instrument.symbol, style = Arta.type.rowName, color = c.on, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                (if (showLocation) pv.location.name + " · " else "") + quantityLine(pv),
                style = Arta.type.caption, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val pl = pv.unrealizedPlBase
            when {
                pv.instrument.assetClass == AssetClass.CASH -> {}
                pl != null -> Text(
                    stringResource(R.string.pl_line, money(pl, cur, compact = true, signed = true), MoneyFormat.signedPct(pv.unrealizedPlPct ?: java.math.BigDecimal.ZERO)),
                    style = Arta.type.small, color = changeColor(pl),
                )
                else -> Text(stringResource(R.string.no_buy_history), style = Arta.type.small, color = c.muted)
            }
        }
        ValueColumn(
            if (pv.missingPrice) stringResource(R.string.no_price) else money(pv.valueBase, cur, compact = true),
            if (pv.instrument.assetClass == AssetClass.CASH || pv.changeTodayBase.signum() == 0 && pv.price?.previousClose == null) "—" else MoneyFormat.signedPct(pv.changeTodayPct),
            if (pv.isStale) c.down else changeColor(pv.changeTodayBase),
        )
    }
}
