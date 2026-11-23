package id.shoko.arta.feature

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import id.shoko.arta.App
import id.shoko.arta.AppContainer

/** Factory ViewModel sederhana untuk DI manual. */
class VmFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = when {
        modelClass.isAssignableFrom(AppViewModel::class.java) -> AppViewModel(container)
        modelClass.isAssignableFrom(id.shoko.arta.feature.advisor.AdvisorViewModel::class.java) -> id.shoko.arta.feature.advisor.AdvisorViewModel(container)
        modelClass.isAssignableFrom(id.shoko.arta.feature.lock.LockViewModel::class.java) -> id.shoko.arta.feature.lock.LockViewModel(container)
        else -> error("ViewModel tidak dikenal: $modelClass")
    } as T
}

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as App).container
