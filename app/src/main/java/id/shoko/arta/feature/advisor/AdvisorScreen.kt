package id.shoko.arta.feature.advisor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import id.shoko.arta.R
import id.shoko.arta.core.money.MoneyFormat
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.ArtaIcons
import id.shoko.arta.core.ui.CircleIconButton
import id.shoko.arta.core.ui.Hairline
import id.shoko.arta.core.ui.LocalHazeState
import id.shoko.arta.core.ui.MutedText
import id.shoko.arta.core.ui.SuggestionChip
import id.shoko.arta.core.ui.frosted
import id.shoko.arta.core.ui.money
import id.shoko.arta.data.repo.ChatMessage
import id.shoko.arta.domain.advisor.AdvisorIntent
import id.shoko.arta.domain.advisor.SimulationCard
import id.shoko.arta.domain.advisor.TemplateResponder
import id.shoko.arta.domain.model.ChatRole
import id.shoko.arta.feature.AppViewModel
import id.shoko.arta.feature.VmFactory
import id.shoko.arta.feature.appContainer
import id.shoko.arta.feature.position.formatTime
import id.shoko.arta.feature.shell.FrostedHeader
import id.shoko.arta.feature.shell.LocalAppActions
import id.shoko.arta.feature.shell.LocalBottomBarSpace
import id.shoko.arta.feature.shell.MainTab
import id.shoko.arta.feature.shell.statusBarTop
import id.shoko.arta.feature.summary.baseCurrency
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class Chip(val label: Int, val intent: AdvisorIntent)

private val chips = listOf(
    Chip(R.string.chip_concentration, AdvisorIntent.CONCENTRATION),
    Chip(R.string.chip_target, AdvisorIntent.TARGET),
    Chip(R.string.chip_usd, AdvisorIntent.USD_EXPOSURE),
)

/**
 * @param embedded true saat tampil sebagai panel kanan di tablet.
 */
@Composable
fun AdvisorScreen(appVm: AppViewModel, embedded: Boolean = false) {
    val container = appContainer()
    val vm: AdvisorViewModel = viewModel(factory = remember { VmFactory(container) })
    val messages by vm.messages.collectAsStateWithLifecycle()
    val thinking by vm.thinking.collectAsStateWithLifecycle()
    val state by appVm.state.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val c = Arta.colors
    val actions = LocalAppActions.current
    val haze = if (embedded) rememberHazeState() else LocalHazeState.current
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    val greeting = remember(state) {
        state?.let { st ->
            val ctx = id.shoko.arta.domain.advisor.AdvisorContext(
                st.valuation, st.settings.concentrationLimit, st.settings.notifyMove, st.data.targets, container.classLabel, asOfIso = "",
            )
            container.advisorService.greeting(ctx).text
        }
    }
    val asked = messages.filter { it.role == ChatRole.USER }.map { it.content }.toSet()
    val chipLabels = chips.map { it to stringResource(it.label) }
    val bottomSpace = if (embedded) 0.dp else LocalBottomBarSpace.current

    LaunchedEffect(messages.size, thinking) {
        val count = messages.size + 1 + if (thinking) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalHazeState provides haze) {
        Box(Modifier.fillMaxSize().background(c.bg).imePadding()) {
            LazyColumn(
                Modifier.fillMaxSize().then(if (haze != null) Modifier.hazeSource(haze) else Modifier),
                state = listState,
                contentPadding = PaddingValues(
                    top = (if (embedded) 0.dp else statusBarTop()) + 88.dp,
                    bottom = bottomSpace + 200.dp,
                    start = 16.dp, end = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item("greeting") { AiMessage(greeting ?: "…", null, onOpenSim = {}) }
                items(messages, key = { it.id }) { m ->
                    if (m.role == ChatRole.USER) UserMessage(m.content)
                    else AiMessage(m.content, m.card, onOpenSim = { card ->
                        appVm.setScenario(card.scenario)
                        actions.selectTab(MainTab.SIMULATION)
                    })
                }
                if (thinking) item("thinking") { TypingBubble() }
            }

            // Header frosted
            Box(Modifier.align(Alignment.TopCenter)) {
                val header: @Composable () -> Unit = {
                    Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircleIconButton(ArtaIcons.history, stringResource(R.string.chat_history), onClick = { showHistory = true }, size = 48.dp)
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.nav_advisor), style = Arta.type.headerTitle, color = c.on)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 3.dp)) {
                                val stale = (state?.valuation?.staleCount ?: 0) > 0
                                Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(if (stale) c.down else c.up))
                                val n = state?.valuation?.byLocation?.size ?: 0
                                val at = state?.valuation?.lastPriceAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH.mm")) } ?: "—"
                                val mode = if (vm.llmConfigured && settings.advisorReadPortfolio) "" else " · " + stringResource(R.string.template_mode)
                                Text(stringResource(R.string.advisor_status, n, at) + mode, style = Arta.type.small, color = c.muted, maxLines = 2, textAlign = TextAlign.Center)
                            }
                        }
                        CircleIconButton(ArtaIcons.newChat, stringResource(R.string.new_chat), onClick = vm::newConversation, size = 48.dp)
                    }
                }
                if (embedded) Column(Modifier.fillMaxWidth().frosted()) { header(); Hairline() } else FrostedHeader { header() }
            }

            // Composer frosted di atas nav
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .padding(bottom = bottomSpace)
                    .then(if (embedded) Modifier else Modifier.navigationBarsPadding())
                    .frosted(),
            ) {
                Hairline()
                Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        chipLabels.filter { (_, label) -> label !in asked }.forEach { (chip, label) ->
                            SuggestionChip(label, onClick = { vm.send(label, chip.intent) })
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(c.surf).padding(start = 18.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val hint = stringResource(R.string.ask_hint)
                        Box(Modifier.weight(1f)) {
                            if (draft.isEmpty()) Text(hint, style = Arta.type.bodyLarge, color = c.muted)
                            BasicTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                textStyle = Arta.type.bodyLarge.copy(color = c.on),
                                cursorBrush = SolidColor(c.accent),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { vm.send(draft); draft = "" }),
                                modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint },
                            )
                        }
                        CircleIconButton(
                            ArtaIcons.send, stringResource(R.string.send),
                            onClick = { vm.send(draft); draft = "" },
                            size = 42.dp, iconSize = 20.dp, background = c.accent, tint = c.onAccent,
                        )
                    }
                    Text(stringResource(R.string.disclaimer), style = Arta.type.small, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    val convos by vm.conversations.collectAsStateWithLifecycle()
    val currentId by vm.conversationId.collectAsStateWithLifecycle()
    HistoryDrawer(
        visible = showHistory,
        conversations = convos,
        currentId = currentId,
        onSelect = vm::openConversation,
        onNew = vm::newConversation,
        onDismiss = { showHistory = false },
    )
}

@Composable
private fun UserMessage(text: String) {
    val c = Arta.colors
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text,
            style = Arta.type.chat.copy(lineHeight = Arta.type.chat.fontSize * 1.45f),
            color = c.onAccent,
            modifier = Modifier.widthIn(max = 300.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp))
                .background(c.accent).padding(horizontal = 15.dp, vertical = 11.dp),
        )
    }
}

@Composable
private fun AiMessage(text: String, card: SimulationCard?, onOpenSim: (SimulationCard) -> Unit) {
    val c = Arta.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(11.dp)).background(c.wash), contentAlignment = Alignment.Center) {
            Icon(ArtaIcons.sparkle, null, Modifier.size(17.dp), tint = c.accent)
        }
        Column(Modifier.weight(1f).padding(top = 5.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text, style = Arta.type.chat, color = c.on)
            card?.let { SimCard(it, onOpenSim) }
        }
    }
}

@Composable
private fun SimCard(card: SimulationCard, onOpen: (SimulationCard) -> Unit) {
    val c = Arta.colors
    val cur = baseCurrency()
    val before = BigDecimal(card.before)
    val after = BigDecimal(card.after)
    val diffPct = BigDecimal(card.diffPct)
    val ratio = if (before.signum() == 0) 1f else (after.toFloat() / before.toFloat()).coerceIn(0f, 1.5f)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.wash).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(card.label.ifBlank { TemplateResponder.scenarioLabel(card.scenario) }, style = Arta.type.caption, color = c.muted, modifier = Modifier.weight(1f))
            Text(MoneyFormat.signedPct(diffPct), style = Arta.type.section.copy(fontSize = Arta.type.rowName.fontSize * 1.06f), color = if (diffPct.signum() < 0) c.down else c.up)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.now), style = Arta.type.caption, color = c.muted)
            Text(money(before, cur, compact = true), style = Arta.type.captionStrong, color = c.on)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.accent))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.after), style = Arta.type.caption, color = c.muted)
            Text(money(after, cur, compact = true), style = Arta.type.captionStrong, color = c.on)
        }
        Row(Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (ratio < 1f) {
                Box(Modifier.weight(ratio.coerceAtLeast(0.01f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.accent))
                Box(Modifier.weight((1f - ratio).coerceAtLeast(0.01f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.down))
            } else {
                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.accent))
                Box(Modifier.weight((ratio - 1f).coerceAtLeast(0.01f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.up))
            }
        }
        Text(
            stringResource(R.string.open_simulation),
            style = Arta.type.labelStrong, color = c.accent,
            modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(c.bg).clickable { onOpen(card) }.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}
