package com.gatelms.ai.transport

import com.gatelms.ai.dto.ChatMessageDto
import com.gatelms.ai.registry.ProviderKind
import com.gatelms.ai.registry.ProviderSpec

interface ChatTransport {
    val kind: ProviderKind

    fun chat(
        spec: ProviderSpec,
        systemInstruction: String,
        messages: List<ChatMessageDto>,
        model: String,
        apiKey: String?
    ): String
}
