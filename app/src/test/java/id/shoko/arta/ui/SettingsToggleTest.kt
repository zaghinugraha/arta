package id.shoko.arta.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import id.shoko.arta.App
import id.shoko.arta.core.security.Authenticator
import id.shoko.arta.core.security.LocalAuthenticator
import id.shoko.arta.core.ui.ArtaTheme
import id.shoko.arta.data.db.ArtaDatabase
import id.shoko.arta.feature.AppViewModel
import id.shoko.arta.feature.lock.LockViewModel
import id.shoko.arta.feature.shell.ArtaApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "w412dp-h891dp-xxhdpi")
class SettingsToggleTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun toggleHideAmounts() {
        val app: App = ApplicationProvider.getApplicationContext()
        app.container.dbHolder.attachForTest(Room.inMemoryDatabaseBuilder(app, ArtaDatabase::class.java).allowMainThreadQueries().build())
        runBlocking { app.container.settings.setOnboardingDone(true) }
        val appVm = AppViewModel(app.container); val lockVm = LockViewModel(app.container)
        compose.setContent { ArtaTheme { CompositionLocalProvider(LocalAuthenticator provides Authenticator { _, ok, _ -> ok() }) { ArtaApp(appVm, lockVm) } } }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Pengaturan").performClick()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Sembunyikan nominal saat dibuka"))
        compose.onNodeWithText("Sembunyikan nominal saat dibuka").performClick()
        compose.waitUntil(5_000) { runBlocking { app.container.settings.current().hideAmountsOnOpen } }
        assertTrue(runBlocking { app.container.settings.current().hideAmountsOnOpen })
        compose.waitForIdle()
        val on = compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "aktif")).fetchSemanticsNodes().size
        compose.onNodeWithText("Sembunyikan nominal saat dibuka").performClick()
        compose.waitUntil(5_000) { !runBlocking { app.container.settings.current().hideAmountsOnOpen } }
        compose.waitForIdle()
        val on2 = compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "aktif")).fetchSemanticsNodes().size
        println("SWITCH_ON_COUNT before=" + on + " after=" + on2)
        org.junit.Assert.assertEquals(on - 1, on2)
    }
}
