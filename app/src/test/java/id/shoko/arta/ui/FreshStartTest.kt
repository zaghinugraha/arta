package id.shoko.arta.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import id.shoko.arta.App
import id.shoko.arta.core.security.Authenticator
import id.shoko.arta.core.security.LocalAuthenticator
import id.shoko.arta.core.ui.ArtaTheme
import id.shoko.arta.data.db.ArtaDatabase
import id.shoko.arta.domain.model.GlassMode
import id.shoko.arta.domain.model.ThemeMode
import id.shoko.arta.feature.AppViewModel
import id.shoko.arta.feature.lock.LockViewModel
import id.shoko.arta.feature.shell.ArtaApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Instal baru: DB kosong, kaca FULL (seperti perangkat kelas atas), alur onboarding + demo. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "w412dp-h891dp-xxhdpi")
class FreshStartTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun freshInstallFullGlass() {
        val app: App = ApplicationProvider.getApplicationContext()
        val db = Room.inMemoryDatabaseBuilder(app, ArtaDatabase::class.java).allowMainThreadQueries().build()
        app.container.dbHolder.attachForTest(db)
        val appVm = AppViewModel(app.container)
        val lockVm = LockViewModel(app.container)
        compose.setContent {
            ArtaTheme(themeMode = ThemeMode.SYSTEM, glassMode = GlassMode.FULL) {
                CompositionLocalProvider(LocalAuthenticator provides Authenticator { _, ok, _ -> ok() }) { ArtaApp(appVm, lockVm) }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Isi data demo").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Total kekayaan").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Total kekayaan").assertExists()
    }
}
