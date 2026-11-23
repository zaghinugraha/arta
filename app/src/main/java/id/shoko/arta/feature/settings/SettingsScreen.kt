package id.shoko.arta.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.hazeSource
import id.shoko.arta.BuildConfig
import id.shoko.arta.R
import id.shoko.arta.core.money.Dec
import id.shoko.arta.core.money.MoneyFormat
import id.shoko.arta.core.money.sumOfDec
import id.shoko.arta.core.security.DeviceSecurity
import id.shoko.arta.core.security.LocalAuthenticator
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.ArtaIcons
import id.shoko.arta.core.ui.ArtaLogoMark
import id.shoko.arta.core.ui.ArtaSwitch
import id.shoko.arta.core.ui.AssetClassRes
import id.shoko.arta.core.ui.LocalHazeState
import id.shoko.arta.core.ui.SettingsIconTile
import id.shoko.arta.data.settings.Settings
import id.shoko.arta.domain.analytics.Allocation
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.GlassMode
import id.shoko.arta.domain.model.GoldReference
import id.shoko.arta.domain.model.ThemeMode
import id.shoko.arta.feature.AppViewModel
import id.shoko.arta.feature.shell.ChoiceDialog
import id.shoko.arta.feature.shell.ConfirmDialog
import id.shoko.arta.feature.shell.FrostedHeader
import id.shoko.arta.feature.shell.InputDialog
import id.shoko.arta.feature.shell.LocalAppActions
import id.shoko.arta.feature.shell.bottomContentSpace
import id.shoko.arta.feature.shell.statusBarTop
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

/** Pasangan tile/ikon dari palet kelas aset. */
private enum class Pal(val cls: AssetClass?) { BLUE(AssetClass.STOCK_IDX), GOLD(AssetClass.GOLD), VIOLET(AssetClass.CRYPTO), TEAL(AssetClass.CASH), PINK(AssetClass.ETF), SLATE(AssetClass.MUTUAL_FUND), RED(null) }

@Composable
private fun palColors(p: Pal): Pair<Color, Color> {
    val c = Arta.colors
    return if (p.cls == null) c.down.copy(alpha = if (c.isDark) 0.14f else 0.08f) to c.down
    else c.classTile(p.cls) to c.classIcon(p.cls)
}

@Composable
fun SettingsScreen(appVm: AppViewModel) {
    val s by appVm.settings.collectAsStateWithLifecycle()
    val state by appVm.state.collectAsStateWithLifecycle()
    val c = Arta.colors
    val haze = LocalHazeState.current
    val actions = LocalAppActions.current
    val auth = LocalAuthenticator.current
    val scope = rememberCoroutineScope()
    val repo = appVm.container.settings
    val context = appVm.container.context
    var dialog by remember { mutableStateOf<String?>(null) }
    fun set(block: suspend () -> Unit) { scope.launch { block() } }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) appVm.exportCsv(uri)
    }
    val authTitle = stringResource(R.string.lock_prompt_title)
    val bioFail = stringResource(R.string.biometric_change_failed)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().then(if (haze != null) Modifier.hazeSource(haze) else Modifier),
            contentPadding = PaddingValues(top = statusBarTop() + 65.dp, bottom = bottomContentSpace()),
        ) {
            item("account") {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp)).background(c.wash).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ArtaLogoMark(52.dp, strokeScale = 0.9f)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.account_title), style = Arta.type.section.copy(fontSize = Arta.type.rowName.fontSize * 1.12f), color = c.on)
                        Text(stringResource(R.string.account_subtitle), style = Arta.type.caption, color = c.muted, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            section(R.string.sec_display) {
                navRow(ArtaIcons.cur, Pal.BLUE, R.string.set_base_currency, if (s.baseCurrency == "IDR") "IDR · Rupiah" else "USD · Dolar AS") { dialog = "currency" }
                navRow(ArtaIcons.theme, Pal.SLATE, R.string.set_theme, themeLabel(s.theme)) { dialog = "theme" }
                navRow(ArtaIcons.lang, Pal.TEAL, R.string.set_language, "Indonesia") { dialog = "language" }
                navRow(ArtaIcons.glass, Pal.SLATE, R.string.set_glass, glassLabel(s.glassMode)) { dialog = "glass" }
            }
            section(R.string.sec_prices) {
                navRow(ArtaIcons.refresh, Pal.TEAL, R.string.set_price_interval, stringResource(R.string.every_n_minutes, s.priceIntervalMin)) { dialog = "interval" }
                navRow(ArtaIcons.gold, Pal.GOLD, R.string.set_gold_ref, stringResource(if (s.goldReference == GoldReference.SPOT) R.string.gold_spot else R.string.gold_buyback)) { dialog = "gold" }
                toggleRow(ArtaIcons.clock, Pal.SLATE, R.string.set_mark_stale, s.markStale) { v -> set { repo.setMarkStale(v) } }
                navRow(ArtaIcons.refresh, Pal.BLUE, R.string.refresh_now, lastRefreshLabel(appVm)) { appVm.refreshNow() }
            }
            section(R.string.sec_limits) {
                navRow(ArtaIcons.pie, Pal.PINK, R.string.set_concentration, stringResource(R.string.pct_per_location, s.concentrationLimitPct)) { dialog = "concentration" }
                val targetCount = state?.data?.targets?.size ?: 0
                navRow(ArtaIcons.target, Pal.VIOLET, R.string.set_targets, if (targetCount == 0) stringResource(R.string.not_set) else stringResource(R.string.n_classes, targetCount)) { dialog = "targets" }
                toggleRow(ArtaIcons.bell, Pal.GOLD, R.string.set_notify_move, s.notifyMoveEnabled, label = stringResource(R.string.notify_move_label, s.notifyMovePct)) { v -> set { repo.setNotifyMoveEnabled(v) } }
                navRow(ArtaIcons.bell, Pal.GOLD, R.string.set_notify_threshold, "> ${s.notifyMovePct}%") { dialog = "threshold" }
            }
            section(R.string.sec_advisor) {
                toggleRow(ArtaIcons.spark, Pal.BLUE, R.string.set_ai_read, s.advisorReadPortfolio) { v -> set { repo.setAdvisorRead(v) } }
                toggleRow(ArtaIcons.list, Pal.VIOLET, R.string.set_ai_daily, s.advisorDailyInsight) { v -> set { repo.setAdvisorDaily(v) } }
                dangerRow(ArtaIcons.trash, R.string.clear_chat) { dialog = "clearChat" }
            }
            section(R.string.sec_security) {
                val canBio = DeviceSecurity.canAuthenticate(context)
                toggleRow(ArtaIcons.lock, Pal.BLUE, R.string.set_biometric, s.biometricLock && canBio, enabled = canBio,
                    sub = if (!canBio) stringResource(R.string.no_device_lock) else null) { v ->
                    auth.authenticate(authTitle, { actions.setBiometric(v) { ok -> if (!ok) appVm.showMessage(bioFail) } }, { appVm.showMessage(it) })
                }
                toggleRow(ArtaIcons.eyeOff, Pal.SLATE, R.string.set_hide_on_open, s.hideAmountsOnOpen) { v -> set { repo.setHideOnOpen(v) } }
                navRow(ArtaIcons.clock, Pal.SLATE, R.string.set_auto_lock, stringResource(R.string.n_minutes, s.autoLockMin)) { dialog = "autolock" }
                val archived = state?.data?.locations?.filter { it.archived }.orEmpty()
                if (archived.isNotEmpty()) navRow(ArtaIcons.list, Pal.SLATE, R.string.archived_locations, archived.joinToString { it.name }) { dialog = "archived" }
                navRow(ArtaIcons.export, Pal.TEAL, R.string.set_export, "CSV") { dialog = "export" }
                if (BuildConfig.DEMO_DATA_AVAILABLE) navRow(ArtaIcons.demo, Pal.VIOLET, R.string.set_demo, stringResource(R.string.set_demo_sub)) { dialog = "demo" }
                dangerRow(ArtaIcons.trash, R.string.delete_all) { dialog = "deleteAll" }
            }
            item("footer") {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 20.dp), horizontalArrangement = Arrangement.Center) {
                    Text("arta", style = Arta.type.rowName.copy(fontWeight = FontWeight.ExtraBold), color = c.on)
                    Text(" v${BuildConfig.VERSION_NAME} · " + stringResource(R.string.footer), style = Arta.type.caption, color = c.muted, textAlign = TextAlign.Center)
                }
            }
        }
        FrostedHeader {
            Box(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                Text(stringResource(R.string.nav_settings), style = Arta.type.pageTitle, color = c.on)
            }
        }
    }

    SettingsDialogs(dialog, s, appVm, onDismiss = { dialog = null }, onExport = {
        exportLauncher.launch("arta-${LocalDate.now()}.csv")
    })
}

@Composable
private fun lastRefreshLabel(appVm: AppViewModel): String {
    val r by appVm.lastRefresh.collectAsStateWithLifecycle()
    val refreshing by appVm.refreshing.collectAsStateWithLifecycle()
    return when {
        refreshing -> stringResource(R.string.refreshing)
        r != null -> stringResource(R.string.last_refresh, id.shoko.arta.feature.position.formatTime(r!!.at))
        else -> stringResource(R.string.refresh_now_sub)
    }
}

@Composable
private fun themeLabel(t: ThemeMode) = stringResource(
    when (t) { ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.DARK -> R.string.theme_dark }
)

@Composable
private fun glassLabel(g: GlassMode) = stringResource(
    when (g) { GlassMode.AUTO -> R.string.glass_auto; GlassMode.FULL -> R.string.glass_full; GlassMode.LIGHT -> R.string.glass_light; GlassMode.OFF -> R.string.glass_off }
)

// ---------- DSL baris ----------

private class SectionScope {
    val rows = mutableListOf<@Composable (divider: Boolean) -> Unit>()
}

private fun LazyListScope.section(title: Int, build: @Composable SectionScope.() -> Unit) {
    item("sec$title") {
        val scope = SectionScope()
        scope.build()
        Column {
            Text(
                stringResource(title), style = Arta.type.sectionLabel, color = Arta.colors.accent,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 6.dp),
            )
            scope.rows.forEachIndexed { i, row -> row(i > 0) }
        }
    }
}

@Composable
private fun RowFrame(divider: Boolean, minHeight: Int, onClick: (() -> Unit)?, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Column {
        if (divider) Box(Modifier.padding(start = 68.dp).fillMaxWidth().height(1.dp).background(Arta.colors.hair))
        Row(
            Modifier.fillMaxWidth().heightIn(min = minHeight.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
        )
    }
}

private fun SectionScope.navRow(icon: ImageVector, pal: Pal, label: Int, value: String, onClick: () -> Unit) {
    rows += { divider ->
        val (tile, tint) = palColors(pal)
        RowFrame(divider, 66, onClick) {
            SettingsIconTile(icon, tile, tint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(label), style = Arta.type.bodyLarge, color = Arta.colors.on)
                Text(value, style = Arta.type.label, color = Arta.colors.muted, maxLines = 2)
            }
        }
    }
}

private fun SectionScope.toggleRow(
    icon: ImageVector, pal: Pal, labelRes: Int, checked: Boolean,
    enabled: Boolean = true, label: String? = null, sub: String? = null, onChange: (Boolean) -> Unit,
) {
    rows += { divider ->
        val (tile, tint) = palColors(pal)
        RowFrame(divider, 58, if (enabled) ({ onChange(!checked) }) else null) {
            SettingsIconTile(icon, tile, tint)
            Column(Modifier.weight(1f)) {
                Text(label ?: stringResource(labelRes), style = Arta.type.bodyLarge, color = if (enabled) Arta.colors.on else Arta.colors.muted)
                sub?.let { Text(it, style = Arta.type.small, color = Arta.colors.muted) }
            }
            ArtaSwitch(checked, onChange, enabled = enabled)
        }
    }
}

private fun SectionScope.dangerRow(icon: ImageVector, label: Int, onClick: () -> Unit) {
    rows += { divider ->
        val (tile, tint) = palColors(Pal.RED)
        RowFrame(divider, 58, onClick) {
            SettingsIconTile(icon, tile, tint)
            Text(stringResource(label), style = Arta.type.bodyLarge, color = Arta.colors.down, modifier = Modifier.weight(1f))
        }
    }
}

// ---------- Dialog ----------

@Composable
private fun SettingsDialogs(dialog: String?, s: Settings, appVm: AppViewModel, onDismiss: () -> Unit, onExport: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repo = appVm.container.settings
    val actions = LocalAppActions.current
    val state by appVm.state.collectAsStateWithLifecycle()
    fun set(block: suspend () -> Unit) { scope.launch { block() } }
    var deleteStep by remember { mutableStateOf(0) }

    when (dialog) {
        "currency" -> ChoiceDialog(stringResource(R.string.set_base_currency), listOf("IDR", "USD"), s.baseCurrency, { it }, { v -> set { repo.setBaseCurrency(v); appVm.refreshNow(true) } }, onDismiss)
        "theme" -> ChoiceDialog(stringResource(R.string.set_theme), ThemeMode.entries, s.theme, { themeLabel(it) }, { v -> set { repo.setTheme(v) } }, onDismiss)
        "language" -> ChoiceDialog(stringResource(R.string.set_language), listOf("id"), "id", { "Indonesia" }, {}, onDismiss, footer = stringResource(R.string.language_note))
        "glass" -> ChoiceDialog(stringResource(R.string.set_glass), GlassMode.entries, s.glassMode, { glassLabel(it) }, { v -> set { repo.setGlassMode(v) } }, onDismiss, footer = stringResource(R.string.glass_note))
        "interval" -> ChoiceDialog(stringResource(R.string.set_price_interval), listOf(5, 15, 30, 60), s.priceIntervalMin, { stringResource(R.string.every_n_minutes, it) }, { v -> set { repo.setPriceInterval(v) } }, onDismiss, footer = stringResource(R.string.interval_note))
        "gold" -> ChoiceDialog(stringResource(R.string.set_gold_ref), GoldReference.entries, s.goldReference,
            { stringResource(if (it == GoldReference.SPOT) R.string.gold_spot_long else R.string.gold_buyback_long) },
            { v -> set { repo.setGoldReference(v); appVm.refreshNow(true) } }, onDismiss, footer = stringResource(R.string.gold_note))
        "concentration" -> ChoiceDialog(stringResource(R.string.set_concentration), (10..50 step 5).toList(), s.concentrationLimitPct, { "$it%" }, { v -> set { repo.setConcentrationLimit(v) } }, onDismiss)
        "threshold" -> ChoiceDialog(stringResource(R.string.set_notify_threshold), listOf(1, 2, 3, 5, 10), s.notifyMovePct, { "> $it%" }, { v -> set { repo.setNotifyMovePct(v) } }, onDismiss)
        "autolock" -> ChoiceDialog(stringResource(R.string.set_auto_lock), listOf(0, 1, 5, 15, 30), s.autoLockMin, { if (it == 0) stringResource(R.string.immediately) else stringResource(R.string.n_minutes, it) }, { v -> set { repo.setAutoLock(v) } }, onDismiss)
        "targets" -> {
            val classes = AssetClass.displayOrder
            val current = state?.data?.targets.orEmpty()
            val labels = classes.map { stringResource(AssetClassRes.label(it)) + " (%)" }
            val totalWrong = stringResource(R.string.targets_must_100)
            InputDialog(
                title = stringResource(R.string.set_targets),
                fields = classes.mapIndexed { i, c -> labels[i] to (current[c]?.let { MoneyFormat.quantity(it, 2) } ?: "") },
                numeric = classes.map { true },
                confirm = stringResource(R.string.save),
                note = stringResource(R.string.targets_note),
                onConfirm = { values ->
                    val map = classes.zip(values).mapNotNull { (c, v) -> Dec.parseUser(v.ifBlank { "0" })?.let { c to it } }.toMap()
                    appVm.setTargets(map)
                },
                onDismiss = onDismiss,
                validate = { values ->
                    val nums = values.map { Dec.parseUser(it.ifBlank { "0" }) }
                    if (nums.any { it == null || it.signum() < 0 }) "Isi angka 0–100"
                    else {
                        val map = classes.zip(nums.map { it!! }).toMap()
                        if (!Allocation.isValidTarget(map)) totalWrong + " (" + MoneyFormat.quantity(nums.map { it!! }.sumOfDec { it }) + "%)" else null
                    }
                },
            )
        }
        "clearChat" -> ConfirmDialog(
            stringResource(R.string.clear_chat), stringResource(R.string.clear_chat_body), stringResource(R.string.delete),
            onConfirm = { scope.launch { appVm.container.advisorRepo.clearAll() } }, onDismiss = onDismiss, danger = true,
        )
        "export" -> ConfirmDialog(
            stringResource(R.string.set_export), stringResource(R.string.export_warning), stringResource(R.string.export_continue),
            onConfirm = onExport, onDismiss = onDismiss,
        )
        "demo" -> ConfirmDialog(
            stringResource(R.string.set_demo), stringResource(R.string.demo_body), stringResource(R.string.set_demo_confirm),
            onConfirm = { appVm.seedDemo() }, onDismiss = onDismiss,
        )
        "archived" -> {
            val archived = state?.data?.locations?.filter { it.archived }.orEmpty()
            ChoiceDialog(stringResource(R.string.archived_locations), archived, null, { it.name }, { actions.openLocation(it.id) }, onDismiss)
        }
        "deleteAll" -> {
            if (deleteStep == 0) ConfirmDialog(
                stringResource(R.string.delete_all), stringResource(R.string.delete_all_body1), stringResource(R.string.continue_),
                onConfirm = { deleteStep = 1 }, onDismiss = { if (deleteStep == 0) onDismiss() }, danger = true,
            ) else ConfirmDialog(
                stringResource(R.string.delete_all_confirm_title), stringResource(R.string.delete_all_body2), stringResource(R.string.delete_all_confirm),
                onConfirm = {
                    scope.launch {
                        appVm.container.wipeEverything()
                        actions.restart()
                    }
                },
                onDismiss = { deleteStep = 0; onDismiss() }, danger = true,
            )
        }
    }
}
