package id.shoko.arta.feature.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.PrimaryButton
import id.shoko.arta.core.ui.SecondaryButton

@Suppress("DEPRECATION")
@Composable
fun CrashScreen(trace: String, onContinue: () -> Unit, onSafeMode: () -> Unit) {
    val c = Arta.colors
    val clipboard = LocalClipboardManager.current
    Box(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Arta berhenti tadi", style = Arta.type.pageTitle, color = c.on)
            Text(
                "Detail error di bawah hanya tersimpan di perangkat ini. Salin dan kirimkan ke pengembang. " +
                    "\"Mode aman\" mematikan efek kaca (blur) lalu membuka aplikasi.",
                style = Arta.type.body, color = c.muted,
            )
            PrimaryButton("Salin detail error", onClick = { clipboard.setText(AnnotatedString(trace)) }, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Buka dengan mode aman", onClick = onSafeMode, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Buka seperti biasa", onClick = onContinue, modifier = Modifier.fillMaxWidth())
            Text(
                trace,
                style = Arta.type.small.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp),
                color = c.on,
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp).clip(RoundedCornerShape(12.dp)).background(c.surf)
                    .horizontalScroll(rememberScrollState()).padding(12.dp),
            )
        }
    }
}
