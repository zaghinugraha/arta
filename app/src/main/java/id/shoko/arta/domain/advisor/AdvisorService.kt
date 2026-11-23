package id.shoko.arta.domain.advisor

import id.shoko.arta.domain.analytics.Scenario
import id.shoko.arta.domain.model.ChatRole
import id.shoko.arta.llm.ChatTurn
import id.shoko.arta.llm.LlmClient
import kotlinx.coroutines.CancellationException

/**
 * Menjawab pertanyaan Advisor.
 * Semua angka dihitung lokal; LLM (bila dikonfigurasi & diizinkan) hanya menyusun kalimat.
 */
class AdvisorService(
    private val llm: LlmClient,
    private val template: TemplateResponder = TemplateResponder(),
) {
    suspend fun answer(
        question: String,
        ctx: AdvisorContext,
        history: List<ChatTurn>,
        allowPortfolioRead: Boolean,
        forcedIntent: AdvisorIntent? = null,
    ): AdvisorReply {
        val parsed = forcedIntent?.let { ParsedQuestion(it) } ?: QuestionParser.parse(question)
        val scenario: Scenario? = parsed.scenario
        val templateReply = template.respond(parsed.intent, ctx, scenario, llmAvailable = llm.isConfigured && allowPortfolioRead)

        val useLlm = llm.isConfigured && allowPortfolioRead && !ctx.isEmpty
        if (!useLlm) return templateReply

        val result = try {
            llm.complete(
                system = PromptBuilder.SYSTEM_PROMPT,
                messages = history.takeLast(MAX_HISTORY) + ChatTurn(ChatRole.USER, PromptBuilder.userMessage(question, ctx, scenario)),
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            // Termasuk NotImplementedError dari placeholder ProviderLlmClient. Jangan log isi/pesan.
            Result.failure(t)
        }
        val text = result.getOrNull()?.takeIf { it.isNotBlank() } ?: return templateReply
        return AdvisorReply(
            text = PromptBuilder.fillPlaceholders(text.trim(), ctx, scenario),
            card = templateReply.card,
            usedLlm = true,
        )
    }

    fun greeting(ctx: AdvisorContext): AdvisorReply = template.respond(AdvisorIntent.GREETING, ctx)

    companion object { const val MAX_HISTORY = 10 }
}
