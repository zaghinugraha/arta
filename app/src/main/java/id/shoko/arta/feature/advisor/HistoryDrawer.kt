package id.shoko.arta.feature.advisor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import id.shoko.arta.R
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.ArtaIcons
import id.shoko.arta.core.ui.ArtaWordmark
import id.shoko.arta.core.ui.MutedText
import id.shoko.arta.data.repo.ChatMessage
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

private val drawerEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/**
 * Riwayat percakapan sebagai panel yang meluncur dari kiri (seperti aplikasi Claude):
 * menutupi seluruh layar dengan scrim, bisa ditutup dengan tap di luar, tombol kembali, atau geser ke kiri.
 */
@Composable
fun HistoryDrawer(
    visible: Boolean,
    conversations: List<ChatMessage>,
    currentId: String,
    onSelect: (String) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    val transition = remember { MutableTransitionState(false) }
    LaunchedEffect(visible) { transition.targetState = visible }
    if (!visible && transition.isIdle && !transition.currentState) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        (LocalView.current.parent as? DialogWindowProvider)?.window?.let { w ->
            LaunchedEffect(w) { w.setDimAmount(0f); w.setWindowAnimations(0) }
        }
        val c = Arta.colors
        val scrim by animateFloatAsState(if (transition.targetState) 0.45f else 0f, tween(300), label = "scrim")
        val drag = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        val panelWidthPx = with(density) { 320.dp.toPx() }

        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim * (1f + drag.value / panelWidthPx).coerceIn(0f, 1f)))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            )
            AnimatedVisibility(
                visibleState = transition,
                enter = slideInHorizontally(tween(340, easing = drawerEasing)) { -it },
                exit = slideOutHorizontally(tween(260, easing = drawerEasing)) { -it },
            ) {
                Column(
                    Modifier.fillMaxHeight().fillMaxWidth(0.84f).widthIn(max = 360.dp)
                        .offset { IntOffset(drag.value.roundToInt(), 0) }
                        .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                        .background(c.bg)
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { d -> scope.launch { drag.snapTo((drag.value + d).coerceAtMost(0f)) } },
                            onDragStopped = { v ->
                                if (drag.value < -panelWidthPx * 0.3f || v < -1500f) onDismiss()
                                scope.launch { drag.animateTo(0f, tween(220)) }
                            },
                        )
                        .statusBarsPadding().navigationBarsPadding(),
                ) {
                    Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        ArtaWordmark()
                    }
                    Row(
                        Modifier.padding(horizontal = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .clickable { onNew(); onDismiss() }.background(c.wash).padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(ArtaIcons.newChat, null, Modifier.size(20.dp), tint = c.accent)
                        Text(stringResource(R.string.new_chat), style = Arta.type.rowName, color = c.accent)
                    }
                    val groups = remember(conversations) { group(conversations) }
                    LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                        if (conversations.isEmpty()) item {
                            MutedText(stringResource(R.string.no_history), Modifier.padding(20.dp), style = Arta.type.body)
                        }
                        groups.forEach { (label, list) ->
                            item("h$label") {
                                Text(stringResource(label), style = Arta.type.sectionLabel, color = c.muted, modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 6.dp))
                            }
                            items(list, key = { it.conversationId }) { m ->
                                val selected = m.conversationId == currentId
                                Text(
                                    m.content,
                                    style = Arta.type.bodyLarge.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                                    color = c.on, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) c.surf else Color.Transparent)
                                        .clickable { onSelect(m.conversationId); onDismiss() }
                                        .padding(horizontal = 12.dp, vertical = 13.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun group(list: List<ChatMessage>): List<Pair<Int, List<ChatMessage>>> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    fun day(m: ChatMessage) = Instant.ofEpochMilli(m.createdAt).atZone(zone).toLocalDate()
    val buckets = list.groupBy { m ->
        val d = day(m)
        when {
            d == today -> R.string.hist_today
            d == today.minusDays(1) -> R.string.hist_yesterday
            d.isAfter(today.minusDays(7)) -> R.string.hist_week
            else -> R.string.hist_older
        }
    }
    return listOf(R.string.hist_today, R.string.hist_yesterday, R.string.hist_week, R.string.hist_older).mapNotNull { k -> buckets[k]?.let { k to it } }
}
