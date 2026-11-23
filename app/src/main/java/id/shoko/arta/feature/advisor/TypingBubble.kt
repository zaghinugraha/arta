package id.shoko.arta.feature.advisor

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.ArtaIcons
import kotlin.math.PI
import kotlin.math.sin

/** Gelembung "sedang mengetik": tiga titik yang naik-turun bergantian. */
@Composable
fun TypingBubble() {
    val c = Arta.colors
    val t = rememberInfiniteTransition(label = "typing")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "phase")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top, modifier = Modifier.semantics { contentDescription = "Advisor sedang mengetik" }) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(11.dp)).background(c.wash), contentAlignment = Alignment.Center) {
            Icon(ArtaIcons.sparkle, null, Modifier.size(17.dp).graphicsLayer { rotationZ = phase * 360f }, tint = c.accent)
        }
        Row(
            Modifier.clip(RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp))
                .background(c.surf).padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { i ->
                val p = ((phase - i * 0.18f) % 1f + 1f) % 1f
                val lift = if (p < 0.5f) sin(p * 2 * PI).toFloat() else 0f
                Box(
                    Modifier.size(8.dp).graphicsLayer {
                        translationY = -lift * 5.dp.toPx()
                        alpha = 0.4f + 0.6f * lift
                    }.clip(CircleShape).background(c.muted)
                )
            }
        }
    }
}
