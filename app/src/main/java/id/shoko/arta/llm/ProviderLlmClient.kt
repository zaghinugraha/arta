package id.shoko.arta.llm

import io.ktor.client.HttpClient

/**
 * PLACEHOLDER — diisi pemilik aplikasi.
 *
 * Selama belum diimplementasikan, Advisor otomatis memakai mode template (TemplateResponder):
 * AdvisorService menangkap kegagalan dari [complete] dan jatuh ke jawaban template berisi angka lokal.
 *
 * Aturan: jangan pernah mencetak API key, isi permintaan, atau respons ke log.
 */
class ProviderLlmClient(private val http: HttpClient) : LlmClient {
    override val isConfigured: Boolean get() = LlmConfig.isConfigured

    override suspend fun complete(system: String, messages: List<ChatTurn>): Result<String> {
        if (!isConfigured) return Result.failure(IllegalStateException("LLM belum dikonfigurasi"))
        // TODO(owner): sesuaikan endpoint, header autentikasi, dan format body/response
        //              dengan dokumentasi provider LLM yang dipilih.
        //              Gunakan LlmConfig.baseUrl, LlmConfig.apiKey, LlmConfig.model.
        //              Contoh kerangka (sesuaikan):
        //                val response = http.post("${LlmConfig.baseUrl}/...") {
        //                    header(<header autentikasi provider>, LlmConfig.apiKey)
        //                    contentType(ContentType.Application.Json)
        //                    setBody(<JSON berisi model, system, messages>)
        //                }
        //                return Result.success(<teks jawaban dari response>)
        TODO("Implementasi request ke provider LLM")
    }
}
