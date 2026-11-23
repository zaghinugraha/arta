package id.shoko.arta.core.ui

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import id.shoko.arta.domain.model.GlassMode
import id.shoko.arta.domain.model.ThemeMode

/** Mode kaca terselesaikan: FULL = blur Haze, LIGHT = semi-opak tanpa blur, OFF = opak. */
enum class ResolvedGlass { FULL, LIGHT, OFF }

val LocalGlass = staticCompositionLocalOf { ResolvedGlass.LIGHT }

/** Nominal disembunyikan (tombol mata). */
val LocalHideAmounts = staticCompositionLocalOf { false }

object Arta {
    val colors: ArtaColors @Composable get() = LocalArtaColors.current
    val type: ArtaType @Composable get() = LocalArtaType.current
}

/**
 * AUTO: blur penuh di perangkat kelas atas; ringan bila low-RAM, RAM < 6 GB, atau refresh rate ≤ 60 Hz.
 * Galaxy Tab A9 (4 GB, 60 Hz) jatuh ke mode ringan.
 */
fun resolveGlass(context: Context, mode: GlassMode): ResolvedGlass = when (mode) {
    GlassMode.FULL -> ResolvedGlass.FULL
    GlassMode.LIGHT -> ResolvedGlass.LIGHT
    GlassMode.OFF -> ResolvedGlass.OFF
    GlassMode.AUTO -> if (isLowEndDevice(context)) ResolvedGlass.LIGHT else ResolvedGlass.FULL
}

fun isLowEndDevice(context: Context): Boolean {
    val am = context.getSystemService(ActivityManager::class.java)
    if (am.isLowRamDevice) return true
    val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    if (mi.totalMem < 6L * 1024 * 1024 * 1024 * 0.93) return true // totalMem sedikit di bawah RAM fisik
    val refresh = runCatching {
        val display = if (Build.VERSION.SDK_INT >= 30) context.display else null
        display?.supportedModes?.maxOfOrNull { it.refreshRate } ?: display?.refreshRate ?: 60f
    }.getOrDefault(60f)
    return refresh <= 61f
}

@Composable
fun ArtaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    glassMode: GlassMode = GlassMode.AUTO,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val c = if (dark) DarkColors else LightColors
    val context = LocalContext.current
    val glass = remember(glassMode) { resolveGlass(context, glassMode) }
    val scheme = if (dark) darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.wash, onPrimaryContainer = c.on,
        secondary = c.accent, onSecondary = c.onAccent, background = c.bg, onBackground = c.on,
        surface = c.bg, onSurface = c.on, surfaceVariant = c.surf, onSurfaceVariant = c.muted,
        surfaceContainer = c.surf, surfaceContainerHigh = c.surf, surfaceContainerHighest = c.surf, surfaceContainerLow = c.bg,
        outline = c.hairStrong, outlineVariant = c.hair, error = c.down, onError = c.onAccent, errorContainer = c.downWash,
    ) else lightColorScheme(
        primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.wash, onPrimaryContainer = c.on,
        secondary = c.accent, onSecondary = c.onAccent, background = c.bg, onBackground = c.on,
        surface = c.bg, onSurface = c.on, surfaceVariant = c.surf, onSurfaceVariant = c.muted,
        surfaceContainer = c.surf, surfaceContainerHigh = c.surf, surfaceContainerHighest = c.surf, surfaceContainerLow = c.bg,
        outline = c.hairStrong, outlineVariant = c.hair, error = c.down, onError = c.onAccent, errorContainer = c.downWash,
    )
    CompositionLocalProvider(
        LocalArtaColors provides c,
        LocalArtaType provides ArtaType(),
        LocalGlass provides glass,
    ) {
        MaterialTheme(colorScheme = scheme, typography = materialTypography(), content = content)
    }
}
