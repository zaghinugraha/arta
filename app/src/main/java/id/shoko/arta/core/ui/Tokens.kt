package id.shoko.arta.core.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import id.shoko.arta.domain.model.AssetClass

/** Token warna dari docs/DESIGN.md §1. */
@Immutable
data class ArtaColors(
    val isDark: Boolean,
    val bg: Color,
    val surf: Color,
    val on: Color,
    val muted: Color,
    val hair: Color,
    val hairStrong: Color,
    val accent: Color,
    val onAccent: Color,
    val wash: Color,
    val up: Color,
    val down: Color,
    val downWash: Color,
    val track: Color,
    val offBorder: Color,
    val fabShadow: Color,
    val frostAlpha: Float,
    private val classColors: Map<AssetClass, Color>,
    private val classTileAlpha: Map<AssetClass, Float>,
) {
    fun classColor(c: AssetClass): Color = classColors.getValue(c)
    fun classTile(c: AssetClass): Color = classColor(c).copy(alpha = classTileAlpha.getValue(c))
    /** Warna ikon pengaturan: versi lebih gelap (terang) / lebih terang (gelap) dari warna kelas. */
    fun classIcon(c: AssetClass): Color = if (isDark) lerp(classColor(c), Color.White, 0.25f) else lerp(classColor(c), Color.Black, 0.3f)
    val frost: Color get() = bg.copy(alpha = frostAlpha)
    val frostLight: Color get() = bg.copy(alpha = 0.94f)

    private fun lerp(a: Color, b: Color, t: Float) = Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = 1f,
    )
}

val LightColors = ArtaColors(
    isDark = false,
    bg = Color(0xFFFCFCFD), surf = Color(0xFFF1F3F6), on = Color(0xFF10131A), muted = Color(0xFF5A6372),
    hair = Color(0xFFE7EAEE), hairStrong = Color(0xFFD3D7DD), accent = Color(0xFF0B6E69), onAccent = Color(0xFFFFFFFF),
    wash = Color(0xFFE3F3F0), up = Color(0xFF15803D), down = Color(0xFFC2362B), downWash = Color(0xFFFBE9E7),
    track = Color(0xFF10131A).copy(alpha = 0.08f), offBorder = Color(0xFF7A828C), fabShadow = Color(0xFF0B6E69).copy(alpha = 0.35f),
    frostAlpha = 0.76f,
    classColors = mapOf(
        AssetClass.STOCK_IDX to Color(0xFF4A67C9), AssetClass.GOLD to Color(0xFFC99A2E), AssetClass.CRYPTO to Color(0xFF7E63CF),
        AssetClass.CASH to Color(0xFF3E9A8C), AssetClass.ETF to Color(0xFFC25E8C), AssetClass.MUTUAL_FUND to Color(0xFF6E7886),
        AssetClass.OTHER to Color(0xFF8C7A6B),
    ),
    classTileAlpha = mapOf(
        AssetClass.STOCK_IDX to 0.10f, AssetClass.GOLD to 0.12f, AssetClass.CRYPTO to 0.10f, AssetClass.CASH to 0.10f,
        AssetClass.ETF to 0.10f, AssetClass.MUTUAL_FUND to 0.10f, AssetClass.OTHER to 0.10f,
    ),
)

val DarkColors = ArtaColors(
    isDark = true,
    bg = Color(0xFF0B0C0F), surf = Color(0xFF181A1F), on = Color(0xFFE9EBEF), muted = Color(0xFF8E96A1),
    hair = Color(0xFF22252B), hairStrong = Color(0xFF343840), accent = Color(0xFF4FD1C5), onAccent = Color(0xFF0B0C0F),
    wash = Color(0xFF0E2624), up = Color(0xFF4CC38A), down = Color(0xFFFF7A6E), downWash = Color(0xFF3A1A17),
    track = Color.White.copy(alpha = 0.10f), offBorder = Color(0xFF7A828C), fabShadow = Color.Black.copy(alpha = 0.5f),
    frostAlpha = 0.74f,
    classColors = mapOf(
        AssetClass.STOCK_IDX to Color(0xFF8AA0F0), AssetClass.GOLD to Color(0xFFE5BE5C), AssetClass.CRYPTO to Color(0xFFAE98F0),
        AssetClass.CASH to Color(0xFF6FCDBE), AssetClass.ETF to Color(0xFFEB9BC0), AssetClass.MUTUAL_FUND to Color(0xFFA4ADB9),
        AssetClass.OTHER to Color(0xFFC2B3A6),
    ),
    classTileAlpha = mapOf(
        AssetClass.STOCK_IDX to 0.15f, AssetClass.GOLD to 0.15f, AssetClass.CRYPTO to 0.15f, AssetClass.CASH to 0.14f,
        AssetClass.ETF to 0.15f, AssetClass.MUTUAL_FUND to 0.14f, AssetClass.OTHER to 0.14f,
    ),
)

val LocalArtaColors = staticCompositionLocalOf { LightColors }
