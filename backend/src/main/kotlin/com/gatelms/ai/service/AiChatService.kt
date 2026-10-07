package com.gatelms.ai.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.dto.AiChatRequest
import com.gatelms.ai.dto.AiChatResponse
import com.gatelms.ai.dto.ChatMessageDto
import com.gatelms.ai.dto.QuestionContextDto
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class AiChatService(
    private val aiProperties: AiProperties,
    private val objectMapper: ObjectMapper,
    restClientBuilder: RestClient.Builder
) {
    private val log = LoggerFactory.getLogger(AiChatService::class.java)
    private val restClient = restClientBuilder.build()

    fun chat(request: AiChatRequest, authHeaderKey: String? = null): AiChatResponse {
        val startTime = System.currentTimeMillis()
        val provider = (request.provider ?: "GROQ").trim().uppercase()
        val effectiveApiKey = resolveApiKey(provider, request.apiKey ?: authHeaderKey)

        val systemInstruction = buildSystemPrompt(request.mode, request.questionContext)

        val responseText = when (provider) {
            "GEMINI" -> callGemini(effectiveApiKey, request.model, systemInstruction, request.messages)
            "OPENAI" -> callOpenAiCompatible(
                endpoint = aiProperties.providers.openai.endpoint.ifBlank { "https://api.openai.com/v1" } + "/chat/completions",
                apiKey = effectiveApiKey,
                model = request.model?.ifBlank { null } ?: aiProperties.providers.openai.defaultModel.ifBlank { "gpt-4o-mini" },
                systemInstruction = systemInstruction,
                messages = request.messages
            )
            "OLLAMA" -> callOllama(
                model = request.model?.ifBlank { null } ?: aiProperties.providers.ollama.defaultModel.ifBlank { "llama3" },
                systemInstruction = systemInstruction,
                messages = request.messages
            )
            else -> { // Default GROQ
                callOpenAiCompatible(
                    endpoint = aiProperties.providers.groq.endpoint.ifBlank { "https://api.groq.com/openai/v1" } + "/chat/completions",
                    apiKey = effectiveApiKey,
                    model = request.model?.ifBlank { null } ?: aiProperties.providers.groq.defaultModel.ifBlank { "llama-3.3-70b-versatile" },
                    systemInstruction = systemInstruction,
                    messages = request.messages
                )
            }
        }

        val latency = System.currentTimeMillis() - startTime
        val resolvedModel = resolveModelName(provider, request.model)

        return AiChatResponse(
            reply = responseText,
            provider = provider,
            model = resolvedModel,
            latencyMs = latency
        )
    }

    private fun resolveApiKey(provider: String, explicitKey: String?): String {
        if (!explicitKey.isNullOrBlank()) return explicitKey.trim()
        val fromProp = when (provider) {
            "GROQ" -> aiProperties.providers.groq.apiKey
            "GEMINI" -> aiProperties.providers.gemini.apiKey
            "OPENAI" -> aiProperties.providers.openai.apiKey
            else -> ""
        }
        if (fromProp.isNotBlank()) return fromProp.trim()

        val envKeyName = "${provider}_API_KEY"
        val envVal = System.getenv(envKeyName) ?: if (provider == "GEMINI") System.getenv("GEMINI_KEY") else null
        if (!envVal.isNullOrBlank()) return envVal.trim()

        if (provider != "OLLAMA") {
            throw IllegalArgumentException("API Key is missing for provider '$provider'. Please set it in BYOK settings or server environment ($envKeyName).")
        }
        return ""
    }

    private fun resolveModelName(provider: String, customModel: String?): String {
        if (!customModel.isNullOrBlank()) return customModel.trim()
        return when (provider) {
            "GEMINI" -> aiProperties.providers.gemini.defaultModel.ifBlank { "gemini-2.0-flash" }
            "OPENAI" -> aiProperties.providers.openai.defaultModel.ifBlank { "gpt-4o-mini" }
            "OLLAMA" -> aiProperties.providers.ollama.defaultModel.ifBlank { "llama3" }
            else -> aiProperties.providers.groq.defaultModel.ifBlank { "llama-3.3-70b-versatile" }
        }
    }

    fun buildSystemPrompt(mode: String?, questionContext: QuestionContextDto?): String {
        val sb = StringBuilder()
        sb.appendLine("You are the GateLMS AI Studio Tutor — an expert, rigorous professor and coach specializing exclusively in the Graduate Aptitude Test in Engineering (GATE) Computer Science and Information Technology examination.")
        sb.appendLine("You have mastery over Algorithms, Data Structures, Theory of Computation, Compiler Design, Operating Systems, Computer Organization and Architecture, Database Management Systems, Computer Networks, Discrete Mathematics, Engineering Mathematics, and General Aptitude.")
        sb.appendLine("Always format mathematical expressions cleanly using LaTeX syntax (\$...\$ for inline formulas and \$\$...\$\$ for block equations), such as asymptotic notations \$O(n \\log n)\$, summation bounds, probabilities, logic connectives, or recurrence relations.")
        sb.appendLine("Keep your tone encouraging, pedagogically sharp, and concise.")

        if (questionContext != null) {
            sb.appendLine("\n--- ACTIVE QUESTION CONTEXT ---")
            if (!questionContext.subjectName.isNullOrBlank()) sb.appendLine("Subject: ${questionContext.subjectName}")
            if (!questionContext.topicName.isNullOrBlank()) sb.appendLine("Topic: ${questionContext.topicName}")
            if (questionContext.year != null) sb.appendLine("Year: GATE CS ${questionContext.year}")
            if (questionContext.type != null) sb.appendLine("Type: ${questionContext.type}")
            if (questionContext.marks != null) sb.appendLine("Marks: ${questionContext.marks}")
            if (!questionContext.statement.isNullOrBlank()) sb.appendLine("Problem Statement:\n${questionContext.statement}")
            if (!questionContext.optionsJson.isNullOrBlank()) sb.appendLine("Options: ${questionContext.optionsJson}")
            if (!questionContext.selectedAnswer.isNullOrBlank()) sb.appendLine("User Selected Answer: ${questionContext.selectedAnswer}")
            sb.appendLine("--------------------------------")
        }

        when (mode?.uppercase()) {
            "HINT" -> {
                sb.appendLine("\nMODE: SOCRATIC HINT ONLY")
                sb.appendLine("- DO NOT reveal the final answer, option letter, or numeric calculation result.")
                sb.appendLine("- Guide the student conceptually: highlight the relevant core theorem, formula, or algorithmic invariant.")
                sb.appendLine("- Point out common pitfalls or edge cases aspirants often miss in this specific GATE topic.")
                sb.appendLine("- Ask 1 targeted question to help them unlock the next deduction step on their own.")
            }
            "EXPLANATION" -> {
                sb.appendLine("\nMODE: RIGOROUS STEP-BY-STEP EXPLANATION")
                sb.appendLine("- Provide a clear, structured mathematical proof or algorithmic derivation.")
                sb.appendLine("- Explain every step thoroughly with intermediate calculations and formulas.")
                sb.appendLine("- Clearly conclude with the final correct answer or numerical interval and justify why incorrect options fail.")
            }
            else -> {
                sb.appendLine("\nMODE: INTERACTIVE TUTORING")
                sb.appendLine("- Answer the student's question directly, accurately, and conceptually.")
                sb.appendLine("- If the student asks for a hint, don't spoil the answer.")
                sb.appendLine("- If the student explains their thought process or working, critique it constructively.")
            }
        }

        return sb.toString()
    }

    private fun callOpenAiCompatible(
        endpoint: String,
        apiKey: String,
        model: String,
        systemInstruction: String,
        messages: List<ChatMessageDto>
    ): String {
        val payloadMessages = mutableListOf<Map<String, String>>()
        payloadMessages.add(mapOf("role" to "system", "content" to systemInstruction))
        for (m in messages) {
            payloadMessages.add(mapOf("role" to m.role, "content" to m.content))
        }

        val requestPayload = mapOf(
            "model" to model,
            "messages" to payloadMessages,
            "temperature" to 0.2
        )

        log.info("Dispatching prompt to OpenAI-compatible endpoint: {} with model: {}", endpoint, model)

        val rawResponse = restClient.post()
            .uri(endpoint)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestPayload)
            .retrieve()
            .body(String::class.java) ?: "{}"

        val root = objectMapper.readTree(rawResponse)
        val choices = root.path("choices")
        if (choices.isArray && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText("").trim()
        }
        val errorNode = root.path("error")
        if (!errorNode.isMissingNode) {
            throw RuntimeException("API Provider Error: ${errorNode.path("message").asText()}")
        }
        return "No response generated."
    }

    private fun callGemini(
        apiKey: String,
        customModel: String?,
        systemInstruction: String,
        messages: List<ChatMessageDto>
    ): String {
        val model = customModel?.ifBlank { null } ?: aiProperties.providers.gemini.defaultModel.ifBlank { "gemini-2.0-flash" }
        val baseUrl = aiProperties.providers.gemini.endpoint.ifBlank { "https://generativelanguage.googleapis.com/v1beta/models" }
        val url = "$baseUrl/$model:generateContent?key=$apiKey"

        val contents = mutableListOf<Map<String, Any>>()
        for (m in messages) {
            val role = if (m.role == "assistant") "model" else "user"
            contents.add(
                mapOf(
                    "role" to role,
                    "parts" to listOf(mapOf("text" to m.content))
                )
            )
        }
        if (contents.isEmpty()) {
            contents.add(mapOf("role" to "user", "parts" to listOf(mapOf("text" to "Hello! Please explain the problem."))))
        }

        val requestPayload = mapOf(
            "system_instruction" to mapOf(
                "parts" to listOf(mapOf("text" to systemInstruction))
            ),
            "contents" to contents,
            "generationConfig" to mapOf("temperature" to 0.2)
        )

        log.info("Dispatching prompt to Google Gemini model: {}", model)

        val rawResponse = restClient.post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestPayload)
            .retrieve()
            .body(String::class.java) ?: "{}"

        val root = objectMapper.readTree(rawResponse)
        val candidates = root.path("candidates")
        if (candidates.isArray && candidates.size() > 0) {
            val parts = candidates.get(0).path("content").path("parts")
            if (parts.isArray && parts.size() > 0) {
                return parts.get(0).path("text").asText("").trim()
            }
        }
        val errorNode = root.path("error")
        if (!errorNode.isMissingNode) {
            throw RuntimeException("Gemini API Error: ${errorNode.path("message").asText()}")
        }
        return "No response generated by Gemini."
    }

    private fun callOllama(
        model: String,
        systemInstruction: String,
        messages: List<ChatMessageDto>
    ): String {
        val baseEndpoint = aiProperties.providers.ollama.endpoint.ifBlank { "http://localhost:11434" }.removeSuffix("/")
        val chatEndpoint = "$baseEndpoint/api/chat"

        val payloadMessages = mutableListOf<Map<String, String>>()
        payloadMessages.add(mapOf("role" to "system", "content" to systemInstruction))
        for (m in messages) {
            payloadMessages.add(mapOf("role" to m.role, "content" to m.content))
        }

        val requestPayload = mapOf(
            "model" to model,
            "messages" to payloadMessages,
            "stream" to false
        )

        log.info("Dispatching prompt to Local Ollama at: {} model: {}", chatEndpoint, model)

        return try {
            val rawResponse = restClient.post()
                .uri(chatEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .body(String::class.java) ?: "{}"

            val root = objectMapper.readTree(rawResponse)
            root.path("message").path("content").asText("").trim()
        } catch (e: Exception) {
            log.error("Local Ollama connection failed: {}", e.message)
            throw RuntimeException("Failed to connect to local Ollama at $baseEndpoint. Ensure Ollama is running (`ollama serve`) with model '$model'.")
        }
    }
}
