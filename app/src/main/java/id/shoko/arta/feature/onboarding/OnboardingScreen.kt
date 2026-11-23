package id.shoko.arta.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.shoko.arta.BuildConfig
import id.shoko.arta.R
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.ArtaLogoMark
import id.shoko.arta.core.ui.MutedText
import id.shoko.arta.core.ui.PrimaryButton
import id.shoko.arta.core.ui.SecondaryButton
import id.shoko.arta.core.ui.TextAction
import id.shoko.arta.domain.model.LocationType
import id.shoko.arta.feature.AppViewModel
import id.shoko.arta.feature.addposition.AddPositionRequest
import id.shoko.arta.feature.addposition.LocationTypePicker
import id.shoko.arta.feature.shell.ArtaTextField
import id.shoko.arta.feature.shell.LocalAppActions
import kotlinx.coroutines.launch

/** Sambutan singkat → buat lokasi pertama → tambah posisi pertama. Bisa dilewati. */
@Composable
fun OnboardingScreen(appVm: AppViewModel) {
    val c = Arta.colors
    val actions = LocalAppActions.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(LocationType.BROKER) }
    val skip = { scope.launch { appVm.container.settings.setOnboardingDone(true) }; Unit }

    Box(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 460.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { TextAction(stringResource(R.string.skip), onClick = skip, color = c.muted) }
            when (step) {
                0 -> {
                    ArtaLogoMark(64.dp)
                    Text(stringResource(R.string.onb_title), style = Arta.type.pageTitle, color = c.on)
                    Text(stringResource(R.string.onb_body), style = Arta.type.body, color = c.muted)
                    Bullet(stringResource(R.string.onb_point1))
                    Bullet(stringResource(R.string.onb_point2))
                    Bullet(stringResource(R.string.onb_point3))
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(stringResource(R.string.onb_start), onClick = { step = 1 }, modifier = Modifier.fillMaxWidth())
                    if (BuildConfig.DEMO_DATA_AVAILABLE) SecondaryButton(stringResource(R.string.set_demo), onClick = { appVm.seedDemo() }, modifier = Modifier.fillMaxWidth())
                }
                1 -> {
                    Text(stringResource(R.string.onb_location_title), style = Arta.type.pageTitle, color = c.on)
                    MutedText(stringResource(R.string.onb_location_body), style = Arta.type.body)
                    ArtaTextField(name, { name = it }, stringResource(R.string.location_name_hint))
                    LocationTypePicker(type) { type = it }
                    PrimaryButton(
                        stringResource(R.string.onb_next),
                        enabled = name.isNotBlank(),
                        onClick = {
                            scope.launch {
                                val id = appVm.addLocationNow(name, type)
                                appVm.container.settings.setOnboardingDone(true)
                                actions.openAddPosition(AddPositionRequest(locationId = id))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Text("•  $text", style = Arta.type.body, color = Arta.colors.on, textAlign = TextAlign.Start)
}
