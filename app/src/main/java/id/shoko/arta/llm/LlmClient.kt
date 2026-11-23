package id.shoko.arta.llm

import id.shoko.arta.domain.model.ChatRole

data class ChatTurn(val role: ChatRole, val content: String)

interface LlmClient {
    val isConfigured: Boolean
    suspend fun complete(system: String, messages: List<ChatTurn>): Result<String>
}
