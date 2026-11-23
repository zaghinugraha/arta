package id.shoko.arta.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.shoko.arta.R
import id.shoko.arta.core.money.MoneyFormat
import id.shoko.arta.domain.model.AssetClass
import java.math.BigDecimal

// ---------- Teks angka ----------

/** Nominal yang ikut tombol mata (persentase tetap terlihat). */
@Composable
fun money(v: BigDecimal, currency: String, compact: Boolean = false, signed: Boolean = false): String {
    if (LocalHideAmounts.current) return if (signed && v.signum() != 0) (if (v.signum() > 0) "+" else MoneyFormat.MINUS) + MoneyFormat.HIDDEN else MoneyFormat.HIDDEN
    return when {
        compact -> MoneyFormat.compact(v, currency, signed)
        signed -> MoneyFormat.signedFull(v, currency)
        else -> MoneyFormat.full(v, currency)
    }
}

@Composable
fun changeColor(v: BigDecimal): Color = when (v.signum()) {
    1 -> Arta.colors.up
    -1 -> Arta.colors.down
    else -> Arta.colors.muted
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Arta.colors.hair))
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text, style = Arta.type.section, color = Arta.colors.on)
        trailing?.invoke()
    }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Arta.colors.accent) {
    Box(
        modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Arta.type.labelStrong, color = color)
    }
}

// ---------- Logo ----------

@Composable
fun ArtaLogoMark(size: Dp = 26.dp, strokeScale: Float = 1f) {
    val c = Arta.colors
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension / 28f
        drawCircle(c.accent, radius = 13f * s, center = Offset(14f * s, 14f * s))
        drawCircle(c.onAccent.copy(alpha = 0.35f), radius = 9.5f * s, center = Offset(14f * s, 14f * s), style = Stroke(1.2f * s))
        val p = Path().apply {
            moveTo(9.5f * s, 18.5f * s); lineTo(14f * s, 8.5f * s); lineTo(18.5f * s, 18.5f * s)
            moveTo(11.3f * s, 15f * s); lineTo(16.7f * s, 15f * s)
        }
        drawPath(p, c.onAccent, style = Stroke(2.2f * s * strokeScale, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ArtaWordmark() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ArtaLogoMark()
        Text("arta", style = Arta.type.wordmark, color = Arta.colors.on)
    }
}

// ---------- Tombol ----------

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    background: Color = Color.Transparent,
    tint: Color = Arta.colors.on,
) {
    Box(
        modifier.size(size).clip(CircleShape).background(background)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(iconSize), tint = tint) }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Arta.colors
    Box(
        modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(26.dp))
            .background(if (enabled) c.accent else c.hairStrong)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Arta.type.rowName.copy(fontWeight = FontWeight.Bold), color = if (enabled) c.onAccent else c.muted)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Arta.colors.on) {
    val c = Arta.colors
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).border(1.dp, c.hairStrong, RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Arta.type.labelStrong, color = color) }
}

/** Pill 36 dp; terpilih = latar aksen. */
@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, minWidth: Dp = 48.dp) {
    val c = Arta.colors
    Box(
        modifier.height(34.dp).widthIn(min = minWidth).clip(RoundedCornerShape(17.dp))
            .background(if (selected) c.accent else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { stateDescription = if (selected) "dipilih" else "" }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = Arta.type.label.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = if (selected) c.onAccent else c.muted,
        )
    }
}

@Composable
fun SuggestionChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Arta.colors
    Row(
        modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, c.hairStrong, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(start = 10.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(ArtaIcons.sparkleSmall, null, Modifier.size(14.dp), tint = c.accent)
        Text(text, style = Arta.type.label, color = c.on)
    }
}

/** Switch 52×32 sesuai mockup. */
@Composable
fun ArtaSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Arta.colors
    val track by animateColorAsState(if (checked) c.accent else Color.Transparent, label = "track")
    val border by animateColorAsState(if (checked) c.accent else c.offBorder, label = "border")
    val thumb by animateDpAsState(if (checked) 24.dp else 16.dp, label = "thumb")
    Box(
        modifier.size(52.dp, 32.dp).clip(CircleShape).background(track).border(2.dp, border, CircleShape)
            .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
            .semantics { stateDescription = if (checked) "aktif" else "nonaktif" }
            .padding(horizontal = 2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier.padding(horizontal = 2.dp).size(thumb).clip(CircleShape).background(if (checked) c.onAccent else c.offBorder),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(ArtaIcons.check, null, Modifier.size(14.dp), tint = c.accent)
        }
    }
}

// ---------- Tile & avatar ----------

@Composable
fun AssetTile(c: AssetClass, size: Dp = 52.dp, illustrationSize: Dp = 41.dp, radius: Dp = 16.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(radius)).background(Arta.colors.classTile(c)),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(AssetClassRes.illustration(c)), null, Modifier.size(illustrationSize))
    }
}

@Composable
fun InitialAvatar(name: String, c: AssetClass, size: Dp = 46.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Arta.colors.classTile(c)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "?",
            style = Arta.type.rowName.copy(fontSize = Arta.type.headerTitle.fontSize * 0.95f, fontWeight = FontWeight.Bold),
            color = Arta.colors.classColor(c),
        )
    }
}

@Composable
fun SettingsIconTile(icon: ImageVector, tile: Color, tint: Color) {
    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tile), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(20.dp), tint = tint)
    }
}

// ---------- Bar ----------

/** Bar porsi kecil (track + isi warna kelas). */
@Composable
fun ShareBar(fraction: Float, color: Color, modifier: Modifier = Modifier.size(84.dp, 5.dp)) {
    Box(modifier.clip(RoundedCornerShape(3.dp)).background(Arta.colors.track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(RoundedCornerShape(3.dp)).background(color))
    }
}

/** Bar bertumpuk alokasi: tinggi 16, gap 3, ujung membulat 8. */
@Composable
fun AllocationBar(parts: List<Pair<AssetClass, Float>>, modifier: Modifier = Modifier, description: String = "") {
    val c = Arta.colors
    val visible = parts.filter { it.second > 0f }
    Row(
        modifier.fillMaxWidth().height(16.dp).semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        visible.forEachIndexed { i, (cls, w) ->
            val shape = when {
                visible.size == 1 -> RoundedCornerShape(8.dp)
                i == 0 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 3.dp, bottomEnd = 3.dp)
                i == visible.lastIndex -> RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                else -> RoundedCornerShape(3.dp)
            }
            Box(Modifier.weight(w).fillMaxHeight().clip(shape).background(c.classColor(cls)))
        }
    }
}

@Composable
fun WarningLabel(text: String) {
    val c = Arta.colors
    Text(
        text,
        style = Arta.type.small.copy(fontWeight = FontWeight.SemiBold),
        color = c.down,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(c.downWash).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

// ---------- Baris ----------

@Composable
fun ValueColumn(value: String, change: String, changeColor: Color) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(value, style = Arta.type.rowValue, color = Arta.colors.on, maxLines = 1)
        Text(change, style = Arta.type.captionStrong, color = changeColor, maxLines = 1)
    }
}

@Composable
fun ListRow(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    minHeight: Dp = 74.dp,
    divider: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = minHeight)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
        if (divider) Hairline()
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ArtaLogoMark(48.dp)
        Text(title, style = Arta.type.section, color = Arta.colors.on, textAlign = TextAlign.Center)
        Text(body, style = Arta.type.body, color = Arta.colors.muted, textAlign = TextAlign.Center)
        action?.let { Spacer(Modifier.height(6.dp)); it() }
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, background: Color = Arta.colors.wash, radius: Dp = 24.dp, padding: PaddingValues = PaddingValues(16.dp), content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(background).padding(padding)) { content() }
}

@Composable
fun OutlinedCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = Arta.colors
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).border(BorderStroke(1.dp, c.hair), RoundedCornerShape(16.dp)).padding(12.dp),
    ) { content() }
}

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier, style: TextStyle = Arta.type.caption, maxLines: Int = Int.MAX_VALUE) {
    Text(text, style = style, color = Arta.colors.muted, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
fun hiddenLabel(): String = stringResource(R.string.amount_hidden)

@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun Spacer16() = Spacer(Modifier.width(16.dp))
