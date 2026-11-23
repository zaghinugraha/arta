package id.shoko.arta.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Grafik garis + area (Canvas, tanpa library). Nilai dinormalisasi ke [0,1] sebelum digambar;
 * konversi ke float hanya untuk posisi piksel, bukan untuk perhitungan uang.
 */
@Composable
fun LineChart(
    normalized: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    description: String = "",
) {
    val pts = remember(normalized) { normalized }
    Canvas(modifier.semantics { contentDescription = description }) {
        if (pts.size < 2) {
            val y = size.height * 0.5f
            drawLine(color.copy(alpha = 0.4f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 2.dp.toPx())
            return@Canvas
        }
        val pad = 3.dp.toPx()
        val h = size.height - pad * 2
        val stepX = size.width / (pts.size - 1)
        val line = Path()
        pts.forEachIndexed { i, v ->
            val x = i * stepX
            val y = pad + (1f - v) * h
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(area, color.copy(alpha = 0.12f))
        drawPath(line, color, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Normalisasi deret ke [0,1]. Deret datar → 0,5. */
fun normalize(values: List<Double>): List<Float> {
    if (values.isEmpty()) return emptyList()
    val min = values.min(); val max = values.max()
    if (max - min < 1e-9) return values.map { 0.5f }
    return values.map { ((it - min) / (max - min)).toFloat() }
}
