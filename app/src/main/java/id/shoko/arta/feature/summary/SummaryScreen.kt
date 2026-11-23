package id.shoko.arta.feature.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.hazeSource
import id.shoko.arta.R
import id.shoko.arta.core.money.MoneyFormat
import id.shoko.arta.core.ui.AllocationBar
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.ArtaIcons
import id.shoko.arta.core.ui.ArtaWordmark
import id.shoko.arta.core.ui.AssetClassRes
import id.shoko.arta.core.ui.AssetTile
import id.shoko.arta.core.ui.CircleIconButton
import id.shoko.arta.core.ui.Dot
import id.shoko.arta.core.ui.EmptyState
import id.shoko.arta.core.ui.Hairline
import id.shoko.arta.core.ui.InitialAvatar
import id.shoko.arta.core.ui.LineChart
import id.shoko.arta.core.ui.ListRow
import id.shoko.arta.core.ui.LocalHazeState
import id.shoko.arta.core.ui.LocalHideAmounts
import id.shoko.arta.core.ui.MutedText
import id.shoko.arta.core.ui.Pill
import id.shoko.arta.core.ui.PrimaryButton
import id.shoko.arta.core.ui.SectionTitle
import id.shoko.arta.core.ui.ShareBar
import id.shoko.arta.core.ui.TextAction
import id.shoko.arta.core.ui.ValueColumn
import id.shoko.arta.core.ui.WarningLabel
import id.shoko.arta.core.ui.changeColor
import id.shoko.arta.core.ui.money
import id.shoko.arta.core.ui.normalize
import id.shoko.arta.data.repo.PortfolioState
import id.shoko.arta.data.repo.SnapshotPoint
import id.shoko.arta.domain.analytics.Insight
import id.shoko.arta.domain.analytics.InsightEngine
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.Location
import id.shoko.arta.domain.valuation.GroupValuation
import id.shoko.arta.feature.AppViewModel
import id.shoko.arta.feature.addposition.AddPositionRequest
import id.shoko.arta.feature.advisor.AdvisorScreen
import id.shoko.arta.feature.shell.FrostedHeader
import id.shoko.arta.feature.shell.LocalAppActions
import id.shoko.arta.feature.shell.LocalWide
import id.shoko.arta.feature.shell.MainTab
import id.shoko.arta.feature.shell.bottomContentSpace
import id.shoko.arta.feature.shell.statusBarTop
import java.math.BigDecimal
import java.time.LocalDate

enum class SummaryTab(val label: Int) { OVERVIEW(R.string.tab_overview), CLASSES(R.string.tab_classes), LOCATIONS(R.string.tab_locations) }

@Composable
fun SummaryScreen(appVm: AppViewModel) {
    val state by appVm.state.collectAsStateWithLifecycle()
    val points by appVm.snapshots.collectAsStateWithLifecycle()
    val refreshing by appVm.refreshing.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(SummaryTab.OVERVIEW) }
    var range by rememberSaveable { mutableStateOf(ChartRange.M1) }
    val wide = LocalWide.current
    val haze = LocalHazeState.current
    val headerHeight = statusBarTop() + 52.dp + 51.dp + 1.dp

    if (wide) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                PullToRefreshBox(isRefreshing = refreshing, onRefresh = { appVm.refreshNow() }, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        Modifier.fillMaxSize().then(if (haze != null) Modifier.hazeSource(haze) else Modifier),
                        contentPadding = PaddingValues(top = headerHeight, bottom = bottomContentSpace()),
                    ) {
                        summaryContent(state, points, tab, range, { range = it }, { tab = it }, wide = true)
                    }
                }
                SummaryHeader(tab, onTab = { tab = it })
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(Arta.colors.hair))
            Box(Modifier.width(392.dp).fillMaxHeight()) { AdvisorScreen(appVm, embedded = true) }
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = { appVm.refreshNow() }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize().then(if (haze != null) Modifier.hazeSource(haze) else Modifier),
                contentPadding = PaddingValues(top = headerHeight, bottom = bottomContentSpace(extra = 96.dp)),
            ) {
                summaryContent(state, points, tab, range, { range = it }, { tab = it }, wide = false)
            }
        }
        SummaryHeader(tab, onTab = { tab = it })
    }
}

@Composable
private fun SummaryHeader(tab: SummaryTab, onTab: (SummaryTab) -> Unit) {
    val c = Arta.colors
    val actions = LocalAppActions.current
    val hidden = LocalHideAmounts.current
    FrostedHeader {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(ArtaIcons.account, stringResource(R.string.account), onClick = { actions.selectTab(MainTab.SETTINGS) }, background = c.surf, iconSize = 20.dp)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { ArtaWordmark() }
            CircleIconButton(
                if (hidden) ArtaIcons.eyeOff else ArtaIcons.eye,
                stringResource(if (hidden) R.string.show_amounts else R.string.hide_amounts),
                onClick = actions.toggleHidden,
            )
        }
        Row(Modifier.fillMaxWidth().height(51.dp)) {
            SummaryTab.entries.forEach { t ->
                val on = t == tab
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Tab) { onTab(t) }.semantics { selected = on },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        stringResource(t.label),
                        style = Arta.type.tab.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                        color = if (on) c.on else c.muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.size(56.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(if (on) c.accent else Color.Transparent))
                }
            }
        }
    }
}

private fun LazyListScope.summaryContent(
    state: PortfolioState?,
    points: List<SnapshotPoint>,
    tab: SummaryTab,
    range: ChartRange,
    onRange: (ChartRange) -> Unit,
    onTab: (SummaryTab) -> Unit,
    wide: Boolean,
) {
    if (state == null) return
    val v = state.valuation
    if (v.positions.isEmpty()) {
        item("empty") { EmptyPortfolio() }
        return
    }
    when (tab) {
        SummaryTab.OVERVIEW -> {
            if (wide) {
                item("top") {
                    Row(Modifier.padding(horizontal = 16.dp).padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.weight(1.2f)) { TotalCard(state, points, range, onRange, Modifier) }
                        Column(Modifier.weight(1f)) { AllocationSection(state.valuation.byClass, onTab, padding = false) }
                    }
                }
                item("stats") { StatsRow(state, points) }
                item("h1") { Hairline() }
                item("insight") { InsightRow(state) }
                item("h2") { Hairline() }
                item("classes-title") { Text(stringResource(R.string.section_classes), style = Arta.type.section, color = Arta.colors.on, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp)) }
                val rows = v.byClass.chunked(2)
                items(rows.size, key = { "cg$it" }) { i ->
                    Row(Modifier.fillMaxWidth()) {
                        rows[i].forEach { g -> Box(Modifier.weight(1f)) { ClassRow(g, v.byClass.maxOf { it.sharePct }) } }
                        if (rows[i].size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                item("total") { TotalCard(state, points, range, onRange, Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) }
                item("stats") { StatsRow(state, points) }
                item("h1") { Hairline() }
                item("insight") { InsightRow(state) }
                item("h2") { Hairline() }
                item("alloc") { AllocationSection(v.byClass, onTab) }
                item("h3") { Hairline() }
                item("classes-title") { Text(stringResource(R.string.section_classes), style = Arta.type.section, color = Arta.colors.on, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp)) }
                val max = v.byClass.maxOf { it.sharePct }
                items(v.byClass, key = { "c" + it.key.code }) { g -> ClassRow(g, max) }
            }
        }
        SummaryTab.CLASSES -> {
            val max = v.byClass.maxOf { it.sharePct }
            items(v.byClass, key = { "c" + it.key.code }) { g -> ClassRow(g, max) }
        }
        SummaryTab.LOCATIONS -> {
            val limit = state.settings.concentrationLimit
            item("loc-summary") {
                val largest = v.byLocation.firstOrNull()
                Column {
                    MutedText(
                        stringResource(R.string.locations_summary, v.byLocation.size, largest?.key?.name ?: "—", MoneyFormat.pct(largest?.sharePct ?: BigDecimal.ZERO)),
                        style = Arta.type.label,
                        modifier = Modifier.padding(16.dp),
                    )
                    Hairline()
                }
            }
            items(v.byLocation, key = { "l" + it.key.id }) { g -> LocationRow(g, limit) }
        }
    }
}

@Composable
private fun EmptyPortfolio() {
    val actions = LocalAppActions.current
    EmptyState(
        title = stringResource(R.string.empty_title),
        body = stringResource(R.string.empty_body),
        action = { PrimaryButton(stringResource(R.string.add_position), onClick = { actions.openAddPosition(AddPositionRequest()) }) },
    )
}

@Composable
fun TotalCard(state: PortfolioState, points: List<SnapshotPoint>, range: ChartRange, onRange: (ChartRange) -> Unit, modifier: Modifier) {
    val c = Arta.colors
    val v = state.valuation
    val cur = v.baseCurrency
    val today = remember { LocalDate.now() }
    val series = remember(points, v.total, range) { SummaryModel.series(points, v.total, today, range) }
    val normalized = remember(series) { normalize(SummaryModel.toDoubles(series)) }
    val chg = v.changeToday
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.wash).padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.total_wealth), style = Arta.type.label, color = c.muted)
            Text(
                cur, style = Arta.type.small.copy(fontWeight = FontWeight.SemiBold), color = c.accent,
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(c.bg).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Text(
            if (LocalHideAmounts.current) "Rp ••••••••" else MoneyFormat.full(v.total, cur),
            style = Arta.type.bigNumber, color = c.on, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val arrow = when (chg.signum()) { 1 -> "↑ "; -1 -> "↓ "; else -> "" }
            Text(
                arrow + money(chg.abs(), cur, signed = false).let { if (chg.signum() < 0) MoneyFormat.MINUS + it else if (chg.signum() > 0) "+$it" else it } +
                    " (" + MoneyFormat.pct(v.changeTodayPct.abs(), 2) + ")",
                style = Arta.type.labelStrong, color = changeColor(chg),
            )
            Text(stringResource(R.string.today), style = Arta.type.label, color = c.muted)
        }
        LineChart(
            normalized, c.accent,
            Modifier.fillMaxWidth().height(74.dp).padding(top = 10.dp),
            description = stringResource(R.string.chart_desc, range.label),
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            ChartRange.entries.forEach { r -> Pill(r.label, r == range, onClick = { onRange(r) }) }
        }
    }
}

@Composable
private fun StatsRow(state: PortfolioState, points: List<SnapshotPoint>) {
    val c = Arta.colors
    val v = state.valuation
    val today = remember { LocalDate.now() }
    val month = remember(points, v.total) { SummaryModel.monthToDate(points, v.total, today) }
    val year = remember(points, v.total) { SummaryModel.yearToDate(points, v.total, today) }
    val stats = listOf(
        Triple(stringResource(R.string.stat_month), month?.let { MoneyFormat.signedPct(it) } ?: "—", month?.let { changeColor(it) } ?: c.muted),
        Triple(stringResource(R.string.stat_year), year?.let { MoneyFormat.signedPct(it) } ?: "—", year?.let { changeColor(it) } ?: c.muted),
        Triple(stringResource(R.string.stat_locations), v.byLocation.size.toString(), c.on),
    )
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)) {
        stats.forEachIndexed { i, (label, value, color) ->
            Row(Modifier.weight(1f)) {
                if (i > 0) Box(Modifier.width(1.dp).height(40.dp).background(c.hair))
                Column(Modifier.padding(start = if (i == 0) 0.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, style = Arta.type.small, color = c.muted)
                    Text(value, style = Arta.type.stat, color = color)
                }
            }
        }
    }
}

@Composable
private fun InsightRow(state: PortfolioState) {
    val c = Arta.colors
    val actions = LocalAppActions.current
    val ctxLabel = classLabelFn()
    if (!state.settings.advisorDailyInsight) return
    val insight = remember(state) {
        InsightEngine.pick(state.valuation, state.settings.concentrationLimit, state.settings.notifyMove, state.data.targets, ctxLabel, state.settings.markStale)
    }
    Row(
        Modifier.fillMaxWidth().clickable { actions.selectTab(MainTab.ADVISOR) }.padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(c.wash), contentAlignment = Alignment.Center) {
            Icon(ArtaIcons.sparkle, null, Modifier.size(20.dp), tint = c.accent)
        }
        Column(Modifier.weight(1f)) {
            Row {
                Text(stringResource(R.string.nav_advisor), style = Arta.type.label.copy(fontWeight = FontWeight.Bold), color = c.on)
                Text(" · " + stringResource(R.string.today), style = Arta.type.label, color = c.muted)
            }
            Text(insight.text, style = Arta.type.body, color = c.on, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(ArtaIcons.chevronRight, null, Modifier.padding(top = 10.dp).size(18.dp), tint = c.muted)
    }
}

@Composable
fun classLabelFn(): (AssetClass) -> String {
    val labels = AssetClass.entries.associateWith { stringResource(AssetClassRes.label(it)) }
    return remember(labels) { { c: AssetClass -> labels.getValue(c) } }
}

@Composable
private fun AllocationSection(byClass: List<GroupValuation<AssetClass>>, onTab: (SummaryTab) -> Unit, padding: Boolean = true) {
    val c = Arta.colors
    val sorted = byClass.sortedBy { AssetClass.displayOrder.indexOf(it.key) }
    val shortLabels = sorted.map { stringResource(AssetClassRes.short(it.key)) }
    val desc = sorted.zip(shortLabels).joinToString { (g, l) -> "$l ${MoneyFormat.pct(g.sharePct)}" }
    Column(Modifier.padding(if (padding) 16.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.section_allocation)) {
            TextAction(stringResource(R.string.per_location), onClick = { onTab(SummaryTab.LOCATIONS) })
        }
        AllocationBar(sorted.map { it.key to it.sharePct.toFloat() }, description = desc)
        sorted.zip(shortLabels).chunked(3).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { (g, label) ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Dot(c.classColor(g.key))
                        Text(label, style = Arta.type.caption, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Text(MoneyFormat.number(g.sharePct.setScale(0, java.math.RoundingMode.HALF_UP)), style = Arta.type.caption.copy(fontWeight = FontWeight.Bold), color = c.on)
                    }
                }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ClassRow(g: GroupValuation<AssetClass>, maxShare: BigDecimal) {
    val c = Arta.colors
    val actions = LocalAppActions.current
    ListRow(onClick = { actions.openClass(g.key) }, minHeight = 78.dp) {
        AssetTile(g.key)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(AssetClassRes.label(g.key)), style = Arta.type.rowName, color = c.on, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val frac = if (maxShare.signum() == 0) 0f else g.sharePct.divide(maxShare, java.math.MathContext.DECIMAL64).toFloat()
                ShareBar(frac, c.classColor(g.key))
                Text(MoneyFormat.pct(g.sharePct), style = Arta.type.caption, color = c.muted)
            }
        }
        ValueColumn(money(g.valueBase, baseCurrency(), compact = true), MoneyFormat.signedPct(g.changeTodayPct), changeColor(g.changeTodayBase))
    }
}

@Composable
private fun LocationRow(g: GroupValuation<Location>, limit: BigDecimal) {
    val c = Arta.colors
    val actions = LocalAppActions.current
    val dominant = g.positions.groupBy { it.instrument.assetClass }.maxByOrNull { (_, l) -> l.sumOf { it.valueBase } }?.key ?: AssetClass.OTHER
    val over = g.sharePct > limit
    ListRow(onClick = { actions.openLocation(g.key.id) }, minHeight = 74.dp) {
        InitialAvatar(g.key.name, dominant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(g.key.name, style = Arta.type.rowName, color = c.on, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(AssetClassRes.label(dominant)) + " · " + MoneyFormat.pct(g.sharePct), style = Arta.type.caption, color = c.muted)
            if (over) Box(Modifier.padding(top = 2.dp)) { WarningLabel(stringResource(R.string.over_limit, MoneyFormat.pct(limit))) }
        }
        ValueColumn(money(g.valueBase, baseCurrency(), compact = true), MoneyFormat.signedPct(g.changeTodayPct), changeColor(g.changeTodayBase))
    }
}

/** Mata uang dasar dari setelan (disediakan ArtaApp). */
@Composable
fun baseCurrency(): String = LocalBaseCurrency.current

val LocalBaseCurrency = androidx.compose.runtime.staticCompositionLocalOf { "IDR" }
