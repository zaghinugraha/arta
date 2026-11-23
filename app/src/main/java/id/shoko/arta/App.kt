package id.shoko.arta

import android.app.Application
import id.shoko.arta.work.MoveNotifier
import id.shoko.arta.work.WorkScheduler
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

open class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        MoveNotifier.ensureChannel(this)
        scheduleBackgroundWork()
    }

    /** Dipisah agar bisa dimatikan di uji JVM (WorkManager tidak tersedia). */
    protected open fun scheduleBackgroundWork() {
        container.appScope.launch {
            container.settings.settings.map { it.priceIntervalMin }.distinctUntilChanged().collect {
                WorkScheduler.schedulePrices(this@App, it)
            }
        }
        WorkScheduler.scheduleSnapshot(this)
    }
}
