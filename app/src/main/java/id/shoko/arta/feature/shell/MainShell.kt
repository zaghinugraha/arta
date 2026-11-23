package id.shoko.arta.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.rememberHazeState
import id.shoko.arta.R
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.ArtaIcons
import id.shoko.arta.core.ui.Hairline
import id.shoko.arta.core.ui.LocalHazeState
import id.shoko.arta.core.ui.frosted
import id.shoko.arta.feature.AppViewModel
import id.shoko.arta.feature.addposition.AddPositionRequest
import id.shoko.arta.feature.advisor.AdvisorScreen
import id.shoko.arta.feature.settings.SettingsScreen
import id.shoko.arta.feature.simulation.SimulationScreen
import id.shoko.arta.feature.summary.SummaryScreen

enum class MainTab { SUMMARY, SIMULATION, ADVISOR, SETTINGS }

/** Ruang yang ditempati bottom nav (tanpa inset sistem). 0 di tablet (rail). */
val LocalBottomBarSpace = staticCompositionLocalOf { 0.dp }
/** true bila tata letak tablet (≥ 840 dp): navigation rail + dua panel. */
val LocalWide = staticCompositionLocalOf { false }

const val NAV_HEIGHT = 64

@Composable
fun isWideLayout(): Boolean = LocalConfiguration.current.screenWidthDp >= 840

@Composable
fun MainShell(appVm: AppViewModel, tab: MainTab, onTab: (MainTab) -> Unit) {
    val wide = isWideLayout()
    val haze = rememberHazeState()
    val actions = LocalAppActions.current
    val showFab = tab == MainTab.SUMMARY || tab == MainTab.SIMULATION
    val openAdd = { actions.openAddPosition(AddPositionRequest()) }

    CompositionLocalProvider(
        LocalHazeState provides haze,
        LocalWide provides wide,
        LocalBottomBarSpace provides if (wide) 0.dp else NAV_HEIGHT.dp,
    ) {
        if (wide) {
            Row(Modifier.fillMaxSize().background(Arta.colors.bg)) {
                NavRail(tab, onTab, onAdd = openAdd)
                Box(Modifier.weight(1f).fillMaxHeight()) { TabContent(appVm, tab) }
            }
        } else {
            Box(Modifier.fillMaxSize().background(Arta.colors.bg)) {
                TabContent(appVm, tab)
                if (showFab) {
                    Fab(
                        onClick = openAdd,
                        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding()
                            .padding(end = 16.dp, bottom = (NAV_HEIGHT + 16).dp),
                    )
                }
                BottomNav(tab, onTab, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun TabContent(appVm: AppViewModel, tab: MainTab) {
    when (tab) {
        MainTab.SUMMARY -> SummaryScreen(appVm)
        MainTab.SIMULATION -> SimulationScreen(appVm)
        MainTab.ADVISOR -> AdvisorScreen(appVm)
        MainTab.SETTINGS -> SettingsScreen(appVm)
    }
}

private data class NavItem(val tab: MainTab, val label: Int, val icon: (Boolean) -> ImageVector)

private val navItems = listOf(
    NavItem(MainTab.SUMMARY, R.string.nav_summary, ArtaIcons::navSummary),
    NavItem(MainTab.SIMULATION, R.string.nav_simulation, ArtaIcons::navSimulation),
    NavItem(MainTab.ADVISOR, R.string.nav_advisor, ArtaIcons::navAdvisor),
    NavItem(MainTab.SETTINGS, R.string.nav_settings, ArtaIcons::navSettings),
)

@Composable
fun Fab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Arta.colors
    val label = stringResource(R.string.add_position)
    Box(
        modifier.size(58.dp)
            .shadow(14.dp, RoundedCornerShape(20.dp), ambientColor = c.fabShadow, spotColor = c.fabShadow)
            .clip(RoundedCornerShape(20.dp)).background(c.accent)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(ArtaIcons.plus, null, Modifier.size(26.dp), tint = c.onAccent) }
}

@Composable
private fun BottomNav(tab: MainTab, onTab: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    val c = Arta.colors
    Column(modifier.fillMaxWidth().frosted()) {
        Hairline()
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(NAV_HEIGHT.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navItems.forEach { item ->
                val active = item.tab == tab
                val label = stringResource(item.label)
                Column(
                    Modifier.size(64.dp, 52.dp).clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Tab, onClickLabel = label) { onTab(item.tab) }
                        .semantics { contentDescription = label; selected = active },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Icon(item.icon(active), null, Modifier.size(25.dp), tint = if (active) c.accent else c.muted)
                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (active) c.accent else Color.Transparent))
                }
            }
        }
    }
}

/** Navigation rail tablet: 92 dp, FAB di atas, ikon + label 12, indikator pill 56×32 latar wash. */
@Composable
private fun NavRail(tab: MainTab, onTab: (MainTab) -> Unit, onAdd: () -> Unit) {
    val c = Arta.colors
    Row {
        Column(
            Modifier.width(92.dp).fillMaxHeight().background(c.bg).statusBarsPadding().navigationBarsPadding().padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Fab(onAdd)
            Spacer(Modifier.height(12.dp))
            navItems.forEach { item ->
                val active = item.tab == tab
                val label = stringResource(item.label)
                Column(
                    Modifier.width(80.dp).clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Tab) { onTab(item.tab) }
                        .semantics { selected = active }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        Modifier.size(56.dp, 32.dp).clip(RoundedCornerShape(16.dp)).background(if (active) c.wash else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) { Icon(item.icon(active), null, Modifier.size(24.dp), tint = if (active) c.accent else c.muted) }
                    Text(label, style = Arta.type.small.copy(fontWeight = if (active) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium), color = if (active) c.on else c.muted)
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(c.hair))
    }
}

/** Header frosted yang menempel di atas (inset status bar sudah termasuk). */
@Composable
fun FrostedHeader(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().frosted()) {
        Column(Modifier.fillMaxWidth().statusBarsPadding()) { content() }
        Hairline()
    }
}

@Composable
fun statusBarTop(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

@Composable
fun navBarBottom(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** Padding bawah konten agar tidak tertutup nav + FAB. */
@Composable
fun bottomContentSpace(extra: Dp = 24.dp): Dp = LocalBottomBarSpace.current + navBarBottom() + extra
