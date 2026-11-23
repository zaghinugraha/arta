package id.shoko.arta.data.repo

import id.shoko.arta.core.time.Clock
import id.shoko.arta.data.db.AdvisorMessageEntity
import id.shoko.arta.data.db.DatabaseHolder
import id.shoko.arta.domain.advisor.SimulationCard
import id.shoko.arta.domain.model.ChatRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: ChatRole,
    val content: String,
    val card: SimulationCard?,
    val createdAt: Long,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AdvisorRepository(private val holder: DatabaseHolder, private val clock: Clock) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observe(conversationId: String): Flow<List<ChatMessage>> =
        holder.db.filterNotNull().flatMapLatest { it.advisorDao().observeConversation(conversationId) }.map { l -> l.map(::toMsg) }

    /** Awal tiap percakapan (pesan pengguna pertama), terbaru dulu — untuk daftar riwayat. */
    val conversations: Flow<List<ChatMessage>> =
        holder.db.filterNotNull().flatMapLatest { it.advisorDao().observeConversationStarts() }.map { l -> l.map(::toMsg) }

    suspend fun add(conversationId: String, role: ChatRole, content: String, card: SimulationCard? = null) {
        holder.require().advisorDao().insert(
            AdvisorMessageEntity(
                UUID.randomUUID().toString(), conversationId, role.name, content,
                card?.let { json.encodeToString<SimulationCard>(it) }, clock.nowMillis(),
            )
        )
    }

    suspend fun observeOnce(conversationId: String): List<ChatMessage> =
        holder.require().advisorDao().getConversation(conversationId).map(::toMsg)

    suspend fun clearAll() = holder.require().advisorDao().clear()

    private fun toMsg(e: AdvisorMessageEntity) = ChatMessage(
        e.id, e.conversationId, runCatching { ChatRole.valueOf(e.role) }.getOrDefault(ChatRole.ASSISTANT), e.content,
        e.cardJson?.let { runCatching { json.decodeFromString<SimulationCard>(it) }.getOrNull() }, e.createdAt,
    )
}
