package id.shoko.arta.core.ui

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import id.shoko.arta.R

/** Plus Jakarta Sans (OFL), dibundel di res/font. */
val Jakarta = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

private const val TNUM = "tnum, lnum"

/** Gaya teks dari docs/DESIGN.md §2. */
@Immutable
data class ArtaType(
    val wordmark: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.05).em, lineHeight = 26.sp),
    val pageTitle: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em),
    val headerTitle: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em, lineHeight = 22.sp),
    val bigNumber: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em, lineHeight = 39.sp, fontFeatureSettings = TNUM),
    val section: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em),
    val rowName: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    val body: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 21.sp),
    val bodyLarge: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 16.sp, fontWeight = FontWeight.Normal),
    val chat: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 15.5.sp, fontWeight = FontWeight.Normal, lineHeight = 24.8.sp),
    val rowValue: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM),
    val caption: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 13.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = TNUM),
    val captionStrong: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM),
    val small: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 12.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = TNUM),
    val label: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 14.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = TNUM),
    val labelStrong: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM),
    val tab: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 15.sp, fontWeight = FontWeight.Medium),
    val sectionLabel: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 13.sp, fontWeight = FontWeight.Bold),
    val stat: TextStyle = TextStyle(fontFamily = Jakarta, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = TNUM),
)

val LocalArtaType = staticCompositionLocalOf { ArtaType() }

fun materialTypography(): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Jakarta),
        displayMedium = base.displayMedium.copy(fontFamily = Jakarta),
        displaySmall = base.displaySmall.copy(fontFamily = Jakarta),
        headlineLarge = base.headlineLarge.copy(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold),
        titleLarge = base.titleLarge.copy(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold),
        titleMedium = base.titleMedium.copy(fontFamily = Jakarta, fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.copy(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontFamily = Jakarta),
        bodyMedium = base.bodyMedium.copy(fontFamily = Jakarta),
        bodySmall = base.bodySmall.copy(fontFamily = Jakarta),
        labelLarge = base.labelLarge.copy(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontFamily = Jakarta),
        labelSmall = base.labelSmall.copy(fontFamily = Jakarta),
    )
}
