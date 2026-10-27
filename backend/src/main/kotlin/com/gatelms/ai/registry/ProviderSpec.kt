package com.gatelms.ai.registry

enum class ProviderKind {
    GEMINI,
    OPENAI_COMPATIBLE,
    OLLAMA
}

data class ProviderSpec(
    var kind: ProviderKind = ProviderKind.OPENAI_COMPATIBLE,
    var label: String? = null,
    var endpoint: String = "",
    var apiKey: String = "",
    var envKeyNames: List<String> = emptyList(),
    var envDefaultModelNames: List<String> = emptyList(),
    var defaultModel: String = "",
    var fallbackModel: String? = null,
    var sttModel: String = "",
    var modelAllowlist: List<String> = emptyList(),
    var modelPrefixes: List<String> = emptyList(),
    var allowAnyModel: Boolean = false,
    var requireKey: Boolean = true,
    var maxRetriesOverride: Int? = null
) {
    fun isModelAllowed(model: String): Boolean {
        val trimmedModel = model.trim()
        if (trimmedModel.isBlank()) return false
        if (allowAnyModel) return true
        if (modelAllowlist.isNotEmpty() && modelAllowlist.any { it.equals(trimmedModel, ignoreCase = true) }) {
            return true
        }
        if (modelPrefixes.isNotEmpty() && modelPrefixes.any { trimmedModel.startsWith(it, ignoreCase = true) }) {
            return true
        }
        if (modelAllowlist.isEmpty() && modelPrefixes.isEmpty()) {
            return true
        }
        return false
    }
}
