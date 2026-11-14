package com.gatelms.ai.transport;

import com.gatelms.ai.dto.ChatMessageDto;
import com.gatelms.ai.registry.ProviderKind;
import com.gatelms.ai.registry.ProviderSpec;
import java.util.List;

public interface ChatTransport {
    ProviderKind getKind();

    String chat(
        ProviderSpec spec,
        String systemInstruction,
        List<ChatMessageDto> messages,
        String model,
        String apiKey
    );
}
