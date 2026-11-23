package id.shoko.arta.feature.advisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.shoko.arta.AppContainer
import id.shoko.arta.data.repo.ChatMessage
import id.shoko.arta.domain.advisor.AdvisorContext
import id.shoko.arta.domain.advisor.AdvisorIntent
import id.shoko.arta.domain.model.ChatRole
import id.shoko.arta.llm.ChatTurn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

class AdvisorViewModel(private val c: AppContainer) : ViewModel() {

    private val _conversationId = MutableStateFlow(UUID.randomUUID().toString())
    val conversationId: StateFlow<String> = _conversationId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = _conversationId.flatMapLatest { c.advisorRepo.observe(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val conversations: StateFlow<List<ChatMessage>> = c.advisorRepo.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()

    val llmConfigured: Boolean get() = c.llmClient.isConfigured

    fun newConversation() { _conversationId.value = UUID.randomUUID().toString() }
    fun openConversation(id: String) { _conversationId.value = id }

    suspend fun context(): AdvisorContext? {
        val st = c.portfolio.currentState()
        return AdvisorContext(
            valuation = st.valuation,
            concentrationLimitPct = st.settings.concentrationLimit,
            notifyMovePct = st.settings.notifyMove,
            targets = st.data.targets,
            classLabel = c.classLabel,
            asOfIso = OffsetDateTime.now(ZoneId.systemDefault()).withNano(0).toString(),
        )
    }

    fun send(text: String, intent: AdvisorIntent? = null) {
        val q = text.trim()
        if (q.isEmpty() || _thinking.value) return
        val convo = _conversationId.value
        viewModelScope.launch {
            _thinking.value = true
            try {
                withContext(Dispatchers.IO) {
                    val history = c.advisorRepo.observeOnce(convo).map { ChatTurn(it.role, it.content) }
                    c.advisorRepo.add(convo, ChatRole.USER, q)
                    val ctx = context() ?: return@withContext
                    val allow = c.settings.current().advisorReadPortfolio
                    val started = System.currentTimeMillis()
                    val reply = c.advisorService.answer(q, ctx, history, allow, intent)
                    // Jawaban template instan; tahan sebentar agar gelembung mengetik tidak sekadar berkedip.
                    val elapsed = System.currentTimeMillis() - started
                    if (elapsed < 650) kotlinx.coroutines.delay(650 - elapsed)
                    c.advisorRepo.add(convo, ChatRole.ASSISTANT, reply.text, reply.card)
                }
            } finally {
                _thinking.value = false
            }
        }
    }
}
