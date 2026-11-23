package id.shoko.arta.feature.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import id.shoko.arta.core.ui.BipolarSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.hazeSource
import id.shoko.arta.R
import id.shoko.arta.core.money.MoneyFormat
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.AssetClassRes
import id.shoko.arta.core.ui.EmptyState
import id.shoko.arta.core.ui.LocalHazeState
import id.shoko.arta.core.ui.MutedText
import id.shoko.arta.core.ui.SecondaryButton
import id.shoko.arta.core.ui.changeColor
import id.shoko.arta.core.ui.money
import id.shoko.arta.domain.analytics.Scenario
import id.shoko.arta.domain.analytics.SimulationEngine
import id.shoko.arta.domain.analytics.UsdExposure
import id.shoko.arta.feature.AppViewModel
import id.shoko.arta.feature.shell.FrostedHeader
import id.shoko.arta.feature.shell.bottomContentSpace
import id.shoko.arta.feature.shell.statusBarTop
import id.shoko.arta.feature.summary.baseCurrency
import java.math.MathContext

@Composable
fun SimulationScreen(appVm: AppViewModel) {
    val state by appVm.state.collectAsStateWithLifecycle()
    val s by appVm.scenario.collectAsStateWithLifecycle()
    val haze = LocalHazeState.current
    val c = Arta.colors
    val v = state?.valuation
    val engine = remember { SimulationEngine() }
    val result = remember(v, s) { v?.let { engine.run(it, s) } }
    val usd = remember(v) { v?.let { UsdExposure.analyze(it) } }
    val cur = baseCurrency()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().then(if (haze != null) Modifier.hazeSource(haze) else Modifier),
            contentPadding = PaddingValues(top = statusBarTop() + 82.dp + 16.dp, bottom = bottomContentSpace(extra = 96.dp), start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (v == null || result == null) return@LazyColumn
            if (v.positions.isEmpty()) {
                item { EmptyState(stringResource(R.string.empty_title), stringResource(R.string.sim_empty)) }
                return@LazyColumn
            }
            item("result") {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.wash).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(stringResource(R.string.sim_after), style = Arta.type.caption, color = c.muted)
                    Text(money(result.totalAfter, cur), style = Arta.type.bigNumber.copy(fontSize = Arta.type.bigNumber.fontSize * 0.82f), color = c.on, maxLines = 1)
                    Text(
                        money(result.diff, cur, signed = true) + " (" + MoneyFormat.signedPct(result.diffPct) + ")",
                        style = Arta.type.labelStrong, color = changeColor(result.diff),
                    )
                    if (usd != null && usd.sharePct.signum() > 0) {
                        Text(stringResource(R.string.sim_usd_sentence, MoneyFormat.pct(usd.sharePct)), style = Arta.type.caption, color = c.muted, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            item("presets") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(stringResource(R.string.preset_market_down), onClick = { appVm.setScenario(Scenario.MARKET_DOWN) }, modifier = Modifier.weight(1f))
                    SecondaryButton(stringResource(R.string.preset_rupiah_weak), onClick = { appVm.setScenario(Scenario.RUPIAH_WEAK) }, modifier = Modifier.weight(1f))
                    SecondaryButton(stringResource(R.string.reset), onClick = { appVm.setScenario(Scenario.RESET) }, color = c.muted)
                }
            }
            item("sliders") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScenarioSlider(stringResource(R.string.sl_crypto), s.cryptoPct, Scenario.CRYPTO_MIN, Scenario.CRYPTO_MAX) { appVm.setScenario(s.copy(cryptoPct = it)) }
                    ScenarioSlider(stringResource(R.string.sl_ihsg), s.ihsgPct, Scenario.IHSG_MIN, Scenario.IHSG_MAX) { appVm.setScenario(s.copy(ihsgPct = it)) }
                    ScenarioSlider(stringResource(R.string.sl_gold), s.goldPct, Scenario.GOLD_MIN, Scenario.GOLD_MAX) { appVm.setScenario(s.copy(goldPct = it)) }
                    ScenarioSlider(stringResource(R.string.sl_usd), s.usdIdrPct, Scenario.USD_MIN, Scenario.USD_MAX) { appVm.setScenario(s.copy(usdIdrPct = it)) }
                }
            }
            item("impact") {
                val maxAbs = result.byClass.maxOfOrNull { it.diff.abs() }?.takeIf { it.signum() > 0 }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hair))
                    Text(stringResource(R.string.sim_impact_title), style = Arta.type.section, color = c.on)
                    result.byClass.forEach { ci ->
                        val frac = if (maxAbs == null) 0f else ci.diff.abs().divide(maxAbs, MathContext.DECIMAL64).toFloat()
                        val label = stringResource(AssetClassRes.label(ci.assetClass))
                        val text = if (ci.diff.abs() < java.math.BigDecimal(50_000)) "—" else money(ci.diff, cur, compact = true, signed = true)
                        Row(
                            Modifier.fillMaxWidth().height(28.dp).semantics { contentDescription = "$label $text" },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(label, style = Arta.type.caption, color = c.muted, modifier = Modifier.width(84.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(Modifier.weight(1f).height(10.dp)) {
                                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
                                    if (ci.diff.signum() < 0) Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp)).background(c.down))
                                }
                                Box(Modifier.width(1.dp).height(14.dp).background(c.hairStrong))
                                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                                    if (ci.diff.signum() > 0) Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp)).background(c.up))
                                }
                            }
                            Text(text, style = Arta.type.captionStrong, color = changeColor(ci.diff), modifier = Modifier.width(96.dp), maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        }
                    }
                    MutedText(stringResource(R.string.sim_model_note), style = Arta.type.small, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        FrostedHeader {
            Column(Modifier.fillMaxWidth().height(81.dp).padding(horizontal = 16.dp), verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.sim_title), style = Arta.type.pageTitle, color = c.on)
                MutedText(stringResource(R.string.sim_subtitle), style = Arta.type.small)
            }
        }
    }
}

@Composable
private fun ScenarioSlider(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    val c = Arta.colors
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = Arta.type.label, color = c.on)
            val color = when { value < 0 -> c.down; value > 0 -> c.up; else -> c.muted }
            androidx.compose.animation.AnimatedContent(value, label = "pct") { v ->
                Text((if (v > 0) "+" else if (v < 0) MoneyFormat.MINUS else "") + kotlin.math.abs(v) + "%", style = Arta.type.labelStrong, color = color)
            }
        }
        BipolarSlider(
            value = value, onValueChange = onChange, min = min, max = max, step = Scenario.STEP, label = label,
        )
    }
}
