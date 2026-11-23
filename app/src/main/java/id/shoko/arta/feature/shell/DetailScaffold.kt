package id.shoko.arta.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import id.shoko.arta.R
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.ArtaIcons
import id.shoko.arta.core.ui.CircleIconButton
import id.shoko.arta.core.ui.LocalHazeState
import id.shoko.arta.core.ui.frosted

/** Kerangka layar detail: header frosted dengan tombol kembali + daftar. */
@Composable
fun DetailScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val haze = rememberHazeState()
    CompositionLocalProvider(LocalHazeState provides haze, LocalBottomBarSpace provides 0.dp) {
        Box(Modifier.fillMaxSize().background(Arta.colors.bg)) {
            LazyColumn(
                Modifier.fillMaxSize().hazeSource(haze),
                contentPadding = PaddingValues(top = statusBarTop() + 57.dp, bottom = navBarBottom() + if (bottomBar != null) 96.dp else 32.dp),
                content = content,
            )
            FrostedHeader {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircleIconButton(ArtaIcons.back, stringResource(R.string.back), onClick = onBack, size = 48.dp)
                    Text(
                        title, style = Arta.type.headerTitle, color = Arta.colors.on, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                    )
                    Box(Modifier.padding(end = 2.dp)) { actions() }
                }
            }
            if (bottomBar != null) {
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().frosted()) {
                    id.shoko.arta.core.ui.Hairline()
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) { bottomBar() }
                }
            }
        }
    }
}
