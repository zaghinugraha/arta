package id.shoko.arta.core.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/** HazeState milik layar saat ini (konten scroll = sumber, bar = efek). */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Permukaan frosted: bg alpha 0,76/0,74 + blur 20 dp + saturasi 180%.
 * Mode ringan: alpha 0,94 tanpa blur. OFF: opak.
 */
@Composable
fun Modifier.frosted(): Modifier {
    val c = Arta.colors
    val glass = LocalGlass.current
    val state = LocalHazeState.current
    if (glass != ResolvedGlass.FULL || state == null) {
        return this.background(if (glass == ResolvedGlass.OFF) c.bg else c.frostLight)
    }
    val tint = c.frost
    val bg = c.bg
    val style = remember(tint, bg) {
        HazeBlurStyle {
            blurRadius(20.dp)
            backgroundColor(bg)
            noiseFactor(0f)
            colorEffects(listOf(HazeColorEffect.tint(tint)))
            fallbackColorEffect(HazeColorEffect.tint(bg.copy(alpha = 0.94f)))
        }
    }
    return this.hazeBlur(HazeInput.Sources(state), style)
}
