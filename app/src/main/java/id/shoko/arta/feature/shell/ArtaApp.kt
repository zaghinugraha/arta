package id.shoko.arta.feature.shell

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.shoko.arta.core.ui.Arta
import id.shoko.arta.core.ui.LocalHideAmounts
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.feature.AppViewModel
import id.shoko.arta.feature.addposition.AddPositionRequest
import id.shoko.arta.feature.addposition.AddPositionSheet
import id.shoko.arta.feature.location.LocationDetailScreen
import id.shoko.arta.feature.lock.LockScreen
import id.shoko.arta.feature.lock.LockState
import id.shoko.arta.feature.lock.LockViewModel
import id.shoko.arta.feature.onboarding.OnboardingScreen
import id.shoko.arta.feature.position.PositionDetailScreen
import id.shoko.arta.feature.summary.AssetClassDetailScreen

/** Navigasi & aksi global yang dipakai banyak layar. */
class AppActions(
    val nav: NavHostController,
    val openAddPosition: (AddPositionRequest) -> Unit,
    val toggleHidden: () -> Unit,
    val selectTab: (MainTab) -> Unit,
    /** Ubah kunci sidik jari (bungkus ulang passphrase). Panggil setelah autentikasi berhasil. */
    val setBiometric: (Boolean, (Boolean) -> Unit) -> Unit,
    /** Setelah "Hapus semua data": kembali ke alur awal. */
    val restart: () -> Unit,
) {
    fun openLocation(id: String) = nav.navigate("location/$id")
    fun openPosition(id: String) = nav.navigate("position/$id")
    fun openClass(c: AssetClass) = nav.navigate("class/${c.code}")
    fun back() { nav.popBackStack() }
}

val LocalAppActions = staticCompositionLocalOf<AppActions> { error("AppActions belum disediakan") }

@Composable
fun ArtaApp(appVm: AppViewModel, lockVm: LockViewModel) {
    val lock by lockVm.state.collectAsStateWithLifecycle()
    val hidden by lockVm.hidden.collectAsStateWithLifecycle()

    if (lock != LockState.Unlocked) {
        LockScreen(lock, lockVm)
        return
    }

    val nav = rememberNavController()
    var addRequest by remember { mutableStateOf<AddPositionRequest?>(null) }
    var tab by rememberSaveable { mutableStateOf(MainTab.SUMMARY) }
    val actions = remember(nav) {
        AppActions(
            nav = nav,
            openAddPosition = { addRequest = it },
            toggleHidden = lockVm::toggleHidden,
            selectTab = { t ->
                tab = t
                nav.popBackStack("main", inclusive = false)
            },
            setBiometric = lockVm::setBiometric,
            restart = lockVm::start,
        )
    }
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val state by appVm.state.collectAsStateWithLifecycle()
    val message by appVm.message.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(message) {
        // Toast sistem: di Samsung otomatis tampil dengan gaya One UI.
        message?.let { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show(); appVm.consumeMessage() }
    }
    LaunchedEffect(Unit) { appVm.onResume() }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(settings.notifyMoveEnabled) {
        if (settings.notifyMoveEnabled && Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val needsOnboarding = state != null && !settings.onboardingDone && state!!.data.locations.isEmpty()

    CompositionLocalProvider(
        LocalHideAmounts provides hidden,
        LocalAppActions provides actions,
        id.shoko.arta.feature.summary.LocalBaseCurrency provides settings.baseCurrency,
    ) {
        Box(Modifier.fillMaxSize().background(Arta.colors.bg)) {
            NavHost(
                navController = nav,
                startDestination = "main",
                // Gaya push One UI: layar baru masuk dari kanan menutupi yang lama (geser sedikit + redup).
                enterTransition = { slideInHorizontally(navTween()) { it } },
                exitTransition = { slideOutHorizontally(navTween()) { -it / 5 } + fadeOut(navTween(), targetAlpha = 0.6f) },
                popEnterTransition = { slideInHorizontally(navTween()) { -it / 5 } + fadeIn(navTween(), initialAlpha = 0.6f) },
                popExitTransition = { slideOutHorizontally(navTween()) { it } },
            ) {
                composable("main") {
                    if (needsOnboarding) OnboardingScreen(appVm)
                    else MainShell(appVm, tab, onTab = { tab = it })
                }
                composable("location/{id}") { e -> LocationDetailScreen(appVm, e.arguments?.getString("id").orEmpty()) }
                composable("position/{id}") { e -> PositionDetailScreen(appVm, e.arguments?.getString("id").orEmpty()) }
                composable("class/{code}") { e -> AssetClassDetailScreen(appVm, AssetClass.fromCode(e.arguments?.getString("code").orEmpty())) }
            }
            addRequest?.let { req ->
                AddPositionSheet(appVm, req, onDismiss = { addRequest = null })
            }
        }
    }
}

/** Kurva 'emphasized decelerate' — terasa seperti transisi sistem. */
private fun <T> navTween() = tween<T>(380, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))
