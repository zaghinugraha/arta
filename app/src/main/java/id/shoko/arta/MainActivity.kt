package id.shoko.arta

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import id.shoko.arta.core.CrashLog
import id.shoko.arta.domain.model.GlassMode
import id.shoko.arta.feature.lock.CrashScreen
import kotlinx.coroutines.launch
import id.shoko.arta.core.security.Authenticator
import id.shoko.arta.core.security.BiometricGate
import id.shoko.arta.core.security.LocalAuthenticator
import id.shoko.arta.core.ui.ArtaTheme
import id.shoko.arta.feature.AppViewModel
import id.shoko.arta.feature.VmFactory
import id.shoko.arta.feature.lock.LockViewModel
import id.shoko.arta.feature.shell.ArtaApp

class MainActivity : FragmentActivity() {

    private val container get() = (application as App).container
    private val factory by lazy { VmFactory(container) }
    private val lockVm by lazy { ViewModelProvider(this, factory)[LockViewModel::class.java] }
    private val appVm by lazy { ViewModelProvider(this, factory)[AppViewModel::class.java] }

    private val processObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) = lockVm.onBackground(System.currentTimeMillis())
        override fun onStart(owner: LifecycleOwner) = lockVm.onForeground(System.currentTimeMillis())
    }

    private val authenticator = Authenticator { title, onSuccess, onError ->
        BiometricGate.prompt(this, title, null, onSuccess) { _, msg -> onError(msg.toString()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver)

        setContent {
            val settings by appVm.settings.collectAsStateWithLifecycle()
            // Tangkapan layar diizinkan secara default (keputusan pemilik); bisa diblokir di Pengaturan.
            LaunchedEffect(settings.blockScreenshots) { applyScreenshotPolicy(settings.blockScreenshots) }
            var crash by remember { mutableStateOf(CrashLog.read(this)) }
            ArtaTheme(themeMode = settings.theme, glassMode = settings.glassMode) {
                CompositionLocalProvider(LocalAuthenticator provides authenticator) {
                    val trace = crash
                    if (trace != null) {
                        CrashScreen(
                            trace = trace,
                            onContinue = { CrashLog.clear(this); crash = null },
                            onSafeMode = {
                                CrashLog.clear(this)
                                lifecycleScope.launch { container.settings.setGlassMode(GlassMode.OFF); crash = null }
                            },
                        )
                    } else {
                        ArtaApp(appVm = appVm, lockVm = lockVm)
                    }
                }
            }
        }
    }

    private fun applyScreenshotPolicy(block: Boolean) {
        if (block) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (android.os.Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(!block)
    }

    override fun onResume() {
        super.onResume()
        if (container.dbHolder.isOpen) appVm.onResume()
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
        super.onDestroy()
    }
}
