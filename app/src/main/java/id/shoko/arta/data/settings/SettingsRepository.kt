package id.shoko.arta.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import id.shoko.arta.domain.model.GlassMode
import id.shoko.arta.domain.model.GoldReference
import id.shoko.arta.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

/** Setelan non-sensitif (bukan untuk rahasia). */
data class Settings(
    val baseCurrency: String = "IDR",
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "id",
    val priceIntervalMin: Int = 15,
    val goldReference: GoldReference = GoldReference.BUYBACK_MANUAL,
    val markStale: Boolean = true,
    val concentrationLimitPct: Int = 25,
    val notifyMoveEnabled: Boolean = true,
    val notifyMovePct: Int = 3,
    val advisorReadPortfolio: Boolean = true,
    val advisorDailyInsight: Boolean = true,
    val biometricLock: Boolean = true,
    val hideAmountsOnOpen: Boolean = false,
    val autoLockMin: Int = 1,
    val glassMode: GlassMode = GlassMode.AUTO,
    val onboardingDone: Boolean = false,
    val lastMoveNotifiedDate: String = "",
) {
    val concentrationLimit: BigDecimal get() = BigDecimal(concentrationLimitPct)
    val notifyMove: BigDecimal get() = BigDecimal(notifyMovePct)

    /** Harga dianggap basi setelah 2× interval. */
    val staleAfterMillis: Long? get() = if (markStale) 2L * priceIntervalMin * 60_000L else null
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object K {
        val baseCurrency = stringPreferencesKey("base_currency")
        val theme = stringPreferencesKey("theme")
        val language = stringPreferencesKey("language")
        val priceInterval = intPreferencesKey("price_interval_min")
        val goldReference = stringPreferencesKey("gold_reference")
        val markStale = booleanPreferencesKey("mark_stale")
        val concentration = intPreferencesKey("concentration_limit_pct")
        val notifyMoveEnabled = booleanPreferencesKey("notify_move_enabled")
        val notifyMove = intPreferencesKey("notify_move_pct")
        val advisorRead = booleanPreferencesKey("advisor_read_portfolio")
        val advisorDaily = booleanPreferencesKey("advisor_daily_insight")
        val biometric = booleanPreferencesKey("biometric_lock")
        val hideOnOpen = booleanPreferencesKey("hide_amounts_on_open")
        val autoLock = intPreferencesKey("auto_lock_min")
        val glass = stringPreferencesKey("glass_mode")
        val onboarding = booleanPreferencesKey("onboarding_done")
        val lastMoveNotified = stringPreferencesKey("last_move_notified_date")
    }

    private inline fun <reified E : Enum<E>> Preferences.enum(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val d = Settings()
        Settings(
            baseCurrency = p[K.baseCurrency] ?: d.baseCurrency,
            theme = p.enum(K.theme, d.theme),
            language = p[K.language] ?: d.language,
            priceIntervalMin = p[K.priceInterval] ?: d.priceIntervalMin,
            goldReference = p.enum(K.goldReference, d.goldReference),
            markStale = p[K.markStale] ?: d.markStale,
            concentrationLimitPct = p[K.concentration] ?: d.concentrationLimitPct,
            notifyMoveEnabled = p[K.notifyMoveEnabled] ?: d.notifyMoveEnabled,
            notifyMovePct = p[K.notifyMove] ?: d.notifyMovePct,
            advisorReadPortfolio = p[K.advisorRead] ?: d.advisorReadPortfolio,
            advisorDailyInsight = p[K.advisorDaily] ?: d.advisorDailyInsight,
            biometricLock = p[K.biometric] ?: d.biometricLock,
            hideAmountsOnOpen = p[K.hideOnOpen] ?: d.hideAmountsOnOpen,
            autoLockMin = p[K.autoLock] ?: d.autoLockMin,
            glassMode = p.enum(K.glass, d.glassMode),
            onboardingDone = p[K.onboarding] ?: d.onboardingDone,
            lastMoveNotifiedDate = p[K.lastMoveNotified] ?: d.lastMoveNotifiedDate,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setBaseCurrency(v: String) = context.dataStore.edit { it[K.baseCurrency] = v }
    suspend fun setTheme(v: ThemeMode) = context.dataStore.edit { it[K.theme] = v.name }
    suspend fun setLanguage(v: String) = context.dataStore.edit { it[K.language] = v }
    suspend fun setPriceInterval(v: Int) = context.dataStore.edit { it[K.priceInterval] = v }
    suspend fun setGoldReference(v: GoldReference) = context.dataStore.edit { it[K.goldReference] = v.name }
    suspend fun setMarkStale(v: Boolean) = context.dataStore.edit { it[K.markStale] = v }
    suspend fun setConcentrationLimit(v: Int) = context.dataStore.edit { it[K.concentration] = v }
    suspend fun setNotifyMoveEnabled(v: Boolean) = context.dataStore.edit { it[K.notifyMoveEnabled] = v }
    suspend fun setNotifyMovePct(v: Int) = context.dataStore.edit { it[K.notifyMove] = v }
    suspend fun setAdvisorRead(v: Boolean) = context.dataStore.edit { it[K.advisorRead] = v }
    suspend fun setAdvisorDaily(v: Boolean) = context.dataStore.edit { it[K.advisorDaily] = v }
    suspend fun setBiometric(v: Boolean) = context.dataStore.edit { it[K.biometric] = v }
    suspend fun setHideOnOpen(v: Boolean) = context.dataStore.edit { it[K.hideOnOpen] = v }
    suspend fun setAutoLock(v: Int) = context.dataStore.edit { it[K.autoLock] = v }
    suspend fun setGlassMode(v: GlassMode) = context.dataStore.edit { it[K.glass] = v.name }
    suspend fun setOnboardingDone(v: Boolean) = context.dataStore.edit { it[K.onboarding] = v }
    suspend fun setLastMoveNotified(date: String) = context.dataStore.edit { it[K.lastMoveNotified] = date }
    suspend fun clearAll() = context.dataStore.edit { it.clear() }
}
