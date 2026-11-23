package id.shoko.arta.feature.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.shoko.arta.AppContainer
import id.shoko.arta.core.security.DeviceSecurity
import id.shoko.arta.core.security.PassphraseStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface LockState {
    data object Checking : LockState
    /** Menunggu sidik jari / kredensial perangkat. */
    data class NeedsAuth(val error: String? = null) : LockState
    data object Unlocked : LockState
    /** Kunci Keystore hilang/invalid atau DB tidak bisa dibuka: data tidak bisa dipulihkan. */
    data object Recovery : LockState
}

class LockViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow<LockState>(LockState.Checking)
    val state: StateFlow<LockState> = _state.asStateFlow()

    /** Nominal disembunyikan (tombol mata). Diatur ulang setiap kali dibuka sesuai setelan. */
    private val _hidden = MutableStateFlow(false)
    val hidden: StateFlow<Boolean> = _hidden.asStateFlow()
    fun toggleHidden() { _hidden.value = !_hidden.value }

    private var backgroundedAt: Long? = null

    init { start() }

    private suspend fun wantAuth(): Boolean = c.settings.current().biometricLock && DeviceSecurity.canAuthenticate(c.context)

    fun start() {
        viewModelScope.launch {
            _state.value = LockState.Checking
            if (c.dbHolder.isOpen) { unlocked(); return@launch }
            val authWrapped = withContext(Dispatchers.IO) { c.passphraseStore.isAuthWrapped() }
            val need = authWrapped == true || (authWrapped == null && wantAuth()) || (authWrapped == false && wantAuth())
            if (need) _state.value = LockState.NeedsAuth() else openDb()
        }
    }

    fun onAuthSucceeded() {
        viewModelScope.launch {
            if (c.dbHolder.isOpen) unlocked() else openDb()
        }
    }

    fun onAuthError(message: String) {
        _state.value = LockState.NeedsAuth(message)
    }

    private suspend fun openDb() {
        val want = wantAuth()
        val result = withContext(Dispatchers.IO) {
            when (val r = c.passphraseStore.loadOrCreate(requireAuthForNew = want)) {
                is PassphraseStore.LoadResult.Ok -> try {
                    c.dbHolder.open(r.passphrase)
                    // Samakan pembungkus passphrase dengan setelan sidik jari (masih dalam jendela autentikasi).
                    if (c.passphraseStore.isAuthWrapped() != want) runCatching { c.passphraseStore.rewrap(r.passphrase, want) }
                    LockState.Unlocked
                } catch (_: Exception) {
                    LockState.Recovery
                } finally {
                    r.passphrase.fill(0)
                }
                PassphraseStore.LoadResult.NeedsAuth -> LockState.NeedsAuth()
                PassphraseStore.LoadResult.Invalidated -> LockState.Recovery
            }
        }
        if (result == LockState.Unlocked) unlocked() else _state.value = result
    }

    private suspend fun unlocked() {
        _hidden.value = c.settings.current().hideAmountsOnOpen
        _state.value = LockState.Unlocked
    }

    /** Hapus semua & mulai ulang (pemulihan). */
    fun resetAll() {
        viewModelScope.launch {
            c.wipeEverything()
            start()
        }
    }

    fun onBackground(now: Long) { backgroundedAt = now }

    /** Kunci otomatis bila kembali dari background > N menit. */
    fun onForeground(now: Long) {
        val at = backgroundedAt ?: return
        backgroundedAt = null
        viewModelScope.launch {
            val s = c.settings.current()
            if (_state.value == LockState.Unlocked && s.biometricLock && DeviceSecurity.canAuthenticate(c.context) &&
                now - at >= s.autoLockMin * 60_000L
            ) {
                _state.value = LockState.NeedsAuth()
            }
        }
    }

    /**
     * Ubah setelan sidik jari: bungkus ulang passphrase. Panggil setelah autentikasi berhasil.
     */
    fun setBiometric(enabled: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                when (val r = c.passphraseStore.loadOrCreate(requireAuthForNew = enabled)) {
                    is PassphraseStore.LoadResult.Ok -> runCatching { c.passphraseStore.rewrap(r.passphrase, enabled) }.isSuccess.also { r.passphrase.fill(0) }
                    else -> false
                }
            }
            if (ok) c.settings.setBiometric(enabled)
            onResult(ok)
        }
    }
}
