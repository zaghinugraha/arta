package id.shoko.arta.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import id.shoko.arta.App
import id.shoko.arta.core.security.Authenticator
import id.shoko.arta.core.security.LocalAuthenticator
import id.shoko.arta.core.ui.ArtaTheme
import id.shoko.arta.data.db.ArtaDatabase
import id.shoko.arta.data.repo.DemoData
import id.shoko.arta.domain.model.GlassMode
import id.shoko.arta.domain.model.ThemeMode
import id.shoko.arta.feature.AppViewModel
import id.shoko.arta.feature.lock.LockViewModel
import id.shoko.arta.feature.shell.ArtaApp
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Application uji: tanpa WorkManager. */
class TestApp : App() {
    override fun scheduleBackgroundWork() = Unit
}

/**
 * Merender layar asli (data demo, DB in-memory tanpa SQLCipher) dan menyimpan PNG ke app/build/screenshots.
 * Dijalankan: ./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*'
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "w412dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var app: App

    @Before fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        val db = Room.inMemoryDatabaseBuilder(app, ArtaDatabase::class.java).allowMainThreadQueries().build()
        app.container.dbHolder.attachForTest(db)
        runBlocking {
            DemoData.seed(db, app.container.clock)
            app.container.settings.setOnboardingDone(true)
            app.container.settings.setGlassMode(GlassMode.LIGHT)
        }
    }

    private fun render(theme: ThemeMode) {
        val appVm = AppViewModel(app.container)
        val lockVm = LockViewModel(app.container)
        compose.setContent {
            ArtaTheme(themeMode = theme, glassMode = GlassMode.LIGHT) {
                CompositionLocalProvider(LocalAuthenticator provides Authenticator { _, ok, _ -> ok() }) {
                    ArtaApp(appVm, lockVm)
                }
            }
        }
        compose.waitForIdle()
    }

    private fun tab(label: String) = compose.onAllNodes(hasText(label) and hasClickAction()).onFirst().performClick()

    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(300)
        compose.waitForIdle()
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun phoneLight() {
        render(ThemeMode.LIGHT)
        shot("01_ringkasan_light")
        tab("Lokasi"); shot("02_lokasi_light")
        tab("Kelas aset"); shot("03_kelas_light")
        compose.onNodeWithContentDescription("Simulasi").performClick()
        compose.onNodeWithText("Pasar turun").performClick(); shot("04_simulasi_light")
        compose.onNodeWithContentDescription("Advisor").performClick(); shot("05_advisor_light")
        compose.onNodeWithText("Cek konsentrasi").performClick(); shot("06_advisor_chip_light")
        compose.onNodeWithContentDescription("Pengaturan").performClick(); shot("07_pengaturan_light")
    }

    @Test fun phoneDark() {
        render(ThemeMode.DARK)
        shot("11_ringkasan_dark")
        tab("Lokasi")
        compose.onAllNodes(hasText("Stockbit") and hasClickAction()).onFirst().performClick(); shot("12_detail_stockbit_dark")
        compose.onAllNodes(hasText("BBCA") and hasClickAction()).onFirst().performClick(); shot("13_detail_bbca_dark")
        compose.onNodeWithContentDescription("Kembali").performClick()
        compose.onNodeWithContentDescription("Kembali").performClick()
        compose.onNodeWithContentDescription("Advisor").performClick()
        compose.onNodeWithText("Eksposur USD").performClick(); shot("14_advisor_dark")
        compose.onNodeWithContentDescription("Pengaturan").performClick(); shot("15_pengaturan_dark")
    }

    @Test fun addPositionSheet() {
        render(ThemeMode.LIGHT)
        compose.onNodeWithContentDescription("Tambah posisi").performClick(); shot("21_tambah_kelas")
        compose.onAllNodesWithText("Kripto").onLast().performClick(); shot("22_tambah_instrumen")
    }

    @Test
    @Config(qualifiers = "w1196dp-h800dp-land-mdpi")
    fun tabletLandscape() {
        render(ThemeMode.LIGHT)
        shot("31_tablet_ringkasan")
    }
}
