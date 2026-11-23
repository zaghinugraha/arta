package id.shoko.arta.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Ikon garis 24×24 dari path SVG di mockup (folder design, file .dc.html). Warna diatur lewat tint. */
object ArtaIcons {
    private fun stroke(name: String, d: String, width: Float = 2f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }.build()

    private fun fill(name: String, vararg d: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            d.forEach { addPath(pathData = PathParser().parsePathString(it).toNodes(), fill = SolidColor(Color.Black)) }
        }.build()

    private const val P_SUMMARY = "M3 3h8v8H3zM13 3h8v5h-8zM13 10h8v11h-8zM3 13h8v8H3z"
    private const val P_SIM = "M4 19h16M7 15l4-5 3 3 5-7"
    private const val P_SPARK = "M11 2l2 5.5L18.5 9.5 13 11.5 11 17l-2-5.5L3.5 9.5 9 7.5zM19 14l.9 2.1 2.1.9-2.1.9-.9 2.1-.9-2.1-2.1-.9 2.1-.9z"
    private const val P_SETTINGS = "M12 8.5a3.5 3.5 0 1 1 0 7a3.5 3.5 0 1 1 0-7zM12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1"

    fun navSummary(active: Boolean) = if (active) navSummaryOn else navSummaryOff
    fun navSimulation(active: Boolean) = if (active) navSimOn else navSimOff
    fun navAdvisor(active: Boolean) = if (active) navAdvisorOn else navAdvisorOff
    fun navSettings(active: Boolean) = if (active) navSettingsOn else navSettingsOff

    private val navSummaryOn = stroke("summaryOn", P_SUMMARY, 2.4f)
    private val navSummaryOff = stroke("summaryOff", P_SUMMARY, 1.8f)
    private val navSimOn = stroke("simOn", P_SIM, 2.4f)
    private val navSimOff = stroke("simOff", P_SIM, 1.8f)
    private val navAdvisorOn = stroke("advOn", P_SPARK, 2.4f)
    private val navAdvisorOff = stroke("advOff", P_SPARK, 1.8f)
    private val navSettingsOn = stroke("setOn", P_SETTINGS, 2.4f)
    private val navSettingsOff = stroke("setOff", P_SETTINGS, 1.8f)

    val sparkle = fill("sparkle", "M11 2l2 5.5L18.5 9.5 13 11.5 11 17l-2-5.5L3.5 9.5 9 7.5z", "M19 14l.9 2.1 2.1.9-2.1.9-.9 2.1-.9-2.1-2.1-.9 2.1-.9z")
    val sparkleSmall = fill("sparkleSmall", "M11 2l2 5.5L18.5 9.5 13 11.5 11 17l-2-5.5L3.5 9.5 9 7.5z")
    val plus = stroke("plus", "M12 5v14M5 12h14", 2.4f)
    val account = stroke("account", "M12 4a4 4 0 1 1 0 8a4 4 0 1 1 0-8zM4 21a8 8 0 0 1 16 0")
    val eye = stroke("eye", "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12zM12 9a3 3 0 1 1 0 6a3 3 0 1 1 0-6z")
    val eyeOff = stroke("eyeOff", "M3 3l18 18M10.6 5.1A10.8 10.8 0 0 1 12 5c6.5 0 10 7 10 7a17 17 0 0 1-3.2 4.1M6.6 6.6C3.7 8.4 2 12 2 12s3.5 7 10 7a9.7 9.7 0 0 0 5.4-1.6")
    val chevronRight = stroke("chevR", "M9 6l6 6-6 6")
    val back = stroke("back", "M15 6l-6 6 6 6")
    val more = fill("more", "M5 10.2a1.8 1.8 0 1 1 0 3.6a1.8 1.8 0 1 1 0-3.6z", "M12 10.2a1.8 1.8 0 1 1 0 3.6a1.8 1.8 0 1 1 0-3.6z", "M19 10.2a1.8 1.8 0 1 1 0 3.6a1.8 1.8 0 1 1 0-3.6z")
    val history = stroke("history", "M4 6h16M4 12h16M4 18h10")
    val newChat = stroke("newChat", "M12 20H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v6M18 15v6M15 18h6")
    val send = stroke("send", "M12 19V5M6 11l6-6 6 6", 2.4f)
    val warning = stroke("warn", "M12 3l10 18H2L12 3zM12 10v5M12 18v.01")
    val check = stroke("check", "M5 12l5 5 9-10", 3f)
    val close = stroke("close", "M6 6l12 12M18 6L6 18")
    val search = stroke("search", "M11 4a7 7 0 1 1 0 14a7 7 0 1 1 0-14zM20 20l-4-4")
    val edit = stroke("edit", "M4 20h4L19 9l-4-4L4 16zM14 6l4 4")
    val refresh = stroke("refresh", "M20 11a8 8 0 1 0-2.3 5.7M20 4v7h-7")
    val fingerprint = stroke("fp", "M12 11v3a8 8 0 0 1-2 5M8 7.5A5 5 0 0 1 17 10v2a12 12 0 0 1-1 5M5 10a7 7 0 0 1 1-3.5M5 14v-3M9 21a10 10 0 0 0 2-6v-3a1 1 0 0 1 2 0v2M19 15v-3a7 7 0 0 0-1.5-4.3")

    // Ikon Pengaturan (FrostPengaturan.dc.html)
    val cur = stroke("cur", "M12 3v18M16 7h-6a3 3 0 0 0 0 6h4a3 3 0 0 1 0 6H8")
    val theme = stroke("theme", "M12 3a9 9 0 1 1 0 18a9 9 0 1 1 0-18zM12 3v18")
    val lang = stroke("lang", "M12 3a9 9 0 1 1 0 18a9 9 0 1 1 0-18zM3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18")
    val gold = stroke("gold", "M4 18l3-8h10l3 8zM8 10l2-4h4l2 4")
    val clock = stroke("clock", "M12 3a9 9 0 1 1 0 18a9 9 0 1 1 0-18zM12 7v5l3 2")
    val pie = stroke("pie", "M12 3a9 9 0 1 0 9 9h-9zM15 3.5A9 9 0 0 1 20.5 9H15z")
    val target = stroke("target", "M12 3a9 9 0 1 1 0 18a9 9 0 1 1 0-18zM12 7a5 5 0 1 1 0 10a5 5 0 1 1 0-10z")
    val bell = stroke("bell", "M6 8a6 6 0 0 1 12 0c0 7 3 8 3 8H3s3-1 3-8M10.3 20a1.9 1.9 0 0 0 3.4 0")
    val spark = stroke("spark", "M11 3l1.8 5L18 10l-5.2 2L11 17l-1.8-5L4 10l5.2-2z")
    val list = stroke("list", "M4 6h16M4 12h10M4 18h13")
    val trash = stroke("trash", "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13")
    val lock = stroke("lock", "M6 11h12v10H6zM8 11V7a4 4 0 0 1 8 0v4")
    val export = stroke("export", "M12 3v12M7 10l5 5 5-5M5 21h14")
    val glass = stroke("glass", "M4 5h16v14H4zM4 9h16")
    val demo = stroke("demo", "M9 3h6M10 3v6l-5 9a2 2 0 0 0 1.7 3h10.6a2 2 0 0 0 1.7-3l-5-9V3")
}
