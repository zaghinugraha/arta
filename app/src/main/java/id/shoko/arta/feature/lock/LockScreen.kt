package id.shoko.arta.feature.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.shoko.arta.R
import id.shoko.arta.core.security.LocalAuthenticator
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.ArtaLogoMark
import id.shoko.arta.core.ui.PrimaryButton
import id.shoko.arta.core.ui.SecondaryButton

@Composable
fun LockScreen(state: LockState, vm: LockViewModel) {
    val c = Arta.colors
    val auth = LocalAuthenticator.current
    val title = stringResource(R.string.lock_prompt_title)
    val prompt = { auth.authenticate(title, { vm.onAuthSucceeded() }, { vm.onAuthError(it) }) }

    if (state is LockState.NeedsAuth && state.error == null) {
        LaunchedEffect(state) { prompt() }
    }

    Box(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ArtaLogoMark(64.dp)
            Text("arta", style = Arta.type.wordmark, color = c.on)
            when (state) {
                LockState.Checking, LockState.Unlocked -> {
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator(Modifier.size(28.dp), color = c.accent, strokeWidth = 3.dp)
                }
                is LockState.NeedsAuth -> {
                    Text(stringResource(R.string.lock_body), style = Arta.type.body, color = c.muted, textAlign = TextAlign.Center)
                    state.error?.let { Text(it, style = Arta.type.caption, color = c.down, textAlign = TextAlign.Center) }
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(stringResource(R.string.lock_unlock), onClick = prompt, modifier = Modifier.fillMaxWidth())
                }
                LockState.Recovery -> RecoveryContent(onReset = vm::resetAll)
            }
        }
    }
}

@Composable
private fun RecoveryContent(onReset: () -> Unit) {
    val c = Arta.colors
    var step by remember { mutableIntStateOf(0) }
    Text(stringResource(R.string.recovery_title), style = Arta.type.section, color = c.on, textAlign = TextAlign.Center)
    Text(stringResource(R.string.recovery_body), style = Arta.type.body, color = c.muted, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    SecondaryButton(stringResource(R.string.recovery_reset), onClick = { step = 1 }, color = c.down, modifier = Modifier.fillMaxWidth())
    if (step == 1) {
        AlertDialog(
            onDismissRequest = { step = 0 },
            title = { Text(stringResource(R.string.recovery_confirm_title)) },
            text = { Text(stringResource(R.string.recovery_confirm_body)) },
            confirmButton = { TextButton(onClick = { step = 0; onReset() }) { Text(stringResource(R.string.delete_all_confirm), color = c.down) } },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
