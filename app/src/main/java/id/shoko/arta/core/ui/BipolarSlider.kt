package id.shoko.arta.core.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Slider dua arah (−/+) bergaya One UI: track tipis membulat, isi dari titik nol ke nilai
 * (merah bila negatif, hijau bila positif), thumb bulat putih, getaran ringan tiap langkah.
 * Thumb mengikuti jari secara kontinu lalu menempel ke langkah terdekat dengan animasi pegas.
 */
@Composable
fun BipolarSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    step: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    val c = Arta.colors
    val view = LocalView.current
    val currentValue by rememberUpdatedState(value)
    val onChange by rememberUpdatedState(onValueChange)
    var dragging by remember { mutableStateOf(false) }
    var dragPos by remember { mutableFloatStateOf(value.toFloat()) }
    val target = if (dragging) dragPos else value.toFloat()
    val shown by animateFloatAsState(
        target,
        if (dragging) spring(stiffness = Spring.StiffnessHigh) else spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "slider",
    )
    val thumbScale by animateFloatAsState(if (dragging) 1.15f else 1f, label = "thumb")

    fun snap(raw: Float): Int = ((raw / step).roundToInt() * step).coerceIn(min, max)
    fun emit(raw: Float) {
        val v = snap(raw)
        if (v != currentValue) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onChange(v)
        }
    }

    Canvas(
        modifier.fillMaxWidth().height(40.dp)
            .semantics {
                contentDescription = label
                stateDescription = "$value%"
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), min.toFloat()..max.toFloat(), (max - min) / step - 1)
                setProgress { t -> emit(t); true }
            }
            .pointerInput(min, max) {
                val pad = 14.dp.toPx()
                fun toValue(x: Float) = min + ((x - pad) / (size.width - 2 * pad)).coerceIn(0f, 1f) * (max - min)
                detectTapGestures { o -> emit(toValue(o.x)) }
            }
            .pointerInput(min, max) {
                val pad = 14.dp.toPx()
                fun toValue(x: Float) = min + ((x - pad) / (size.width - 2 * pad)).coerceIn(0f, 1f) * (max - min)
                detectHorizontalDragGestures(
                    onDragStart = { o -> dragging = true; dragPos = toValue(o.x); emit(dragPos) },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, _ ->
                    change.consume()
                    dragPos = toValue(change.position.x)
                    emit(dragPos)
                }
            },
    ) {
        val pad = 14.dp.toPx()
        val w = size.width - 2 * pad
        val cy = size.height / 2
        val trackH = 6.dp.toPx()
        fun xOf(v: Float) = pad + (v - min) / (max - min) * w
        // Track
        drawRoundRect(c.track, Offset(pad, cy - trackH / 2), Size(w, trackH), CornerRadius(trackH / 2))
        // Titik langkah (halus)
        var s = min
        while (s <= max) {
            if (s != 0) drawCircle(c.muted.copy(alpha = 0.25f), 1.5.dp.toPx(), Offset(xOf(s.toFloat()), cy))
            s += step
        }
        // Isi dari nol
        val x0 = xOf(0f)
        val xv = xOf(shown)
        val fill = when {
            shown < -0.01f -> c.down
            shown > 0.01f -> c.up
            else -> Color.Transparent
        }
        if (abs(xv - x0) > 0.5f) {
            drawRoundRect(fill, Offset(minOf(x0, xv), cy - trackH / 2), Size(abs(xv - x0), trackH), CornerRadius(trackH / 2))
        }
        // Penanda nol
        drawRoundRect(c.muted.copy(alpha = 0.6f), Offset(x0 - 1.dp.toPx(), cy - 7.dp.toPx()), Size(2.dp.toPx(), 14.dp.toPx()), CornerRadius(1.dp.toPx()))
        // Thumb
        val r = 11.dp.toPx() * thumbScale
        drawCircle(Color.Black.copy(alpha = if (c.isDark) 0.4f else 0.12f), r + 1.5.dp.toPx(), Offset(xv, cy + 1.dp.toPx()))
        drawCircle(if (c.isDark) Color(0xFFE9EBEF) else Color.White, r, Offset(xv, cy))
        drawCircle(if (fill == Color.Transparent) c.accent else fill, r * 0.36f, Offset(xv, cy))
    }
}
