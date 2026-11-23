package id.shoko.arta.llm

import id.shoko.arta.BuildConfig

/** Konfigurasi LLM dari BuildConfig (diisi dari local.properties). Jangan pernah me-log nilainya. */
object LlmConfig {
    val apiKey: String get() = BuildConfig.LLM_API_KEY
    val baseUrl: String get() = BuildConfig.LLM_BASE_URL
    val model: String get() = BuildConfig.LLM_MODEL
    val isConfigured: Boolean get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()
}
