package com.gatelms.ai.service;

import com.gatelms.ai.dto.*;
import com.gatelms.ai.exception.AiExceptions.*;
import com.gatelms.ai.registry.ProviderRegistry;
import com.gatelms.ai.registry.ProviderSpec;
import com.gatelms.ai.transport.ChatTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private final ProviderRegistry providerRegistry;
    private final Map<String, ChatTransport> transportMap = new HashMap<>();

    public AiChatService(ProviderRegistry providerRegistry, List<ChatTransport> transports) {
        this.providerRegistry = providerRegistry;
        for (ChatTransport t : transports) {
            transportMap.put(t.getKind().name(), t);
        }
    }

    public AiChatResponse chat(AiChatRequest request) {
        return chat(request, null, null);
    }

    public AiChatResponse chat(AiChatRequest request, String customApiKey) {
        return chat(request, customApiKey, null);
    }

    public AiChatResponse chat(AiChatRequest request, String customApiKey, String customProvider) {
        String providerId = (customProvider != null && !customProvider.trim().isEmpty())
            ? customProvider.trim().toLowerCase()
            : (request.provider() != null && !request.provider().trim().isEmpty())
                ? request.provider().trim().toLowerCase()
                : "gemini";

        ProviderSpec spec = providerRegistry.get(providerId)
            .orElseThrow(() -> new AiValidationException("Unknown or unsupported AI provider: " + providerId));

        String apiKey = providerRegistry.resolveApiKey(providerId, customApiKey);
        if (spec.isRequireKey() && (apiKey == null || apiKey.trim().isEmpty())) {
            throw new AiValidationException("Missing API key for provider '" + spec.getLabel() + "'. Please configure in Settings or set environment variable.");
        }

        String candidateModel = (request.model() != null && !request.model().trim().isEmpty())
            ? request.model().trim()
            : providerRegistry.resolveDefaultModel(providerId);

        if (!spec.isAllowAnyModel()) {
            final String checkModel = candidateModel;
            boolean matchesPrefix = spec.getModelPrefixes().stream().anyMatch(p -> checkModel.toLowerCase().startsWith(p.toLowerCase()));
            boolean inAllowlist = spec.getModelAllowlist().stream().anyMatch(m -> m.equalsIgnoreCase(checkModel));
            if (!matchesPrefix && !inAllowlist) {
                candidateModel = spec.getDefaultModel();
            }
        }
        String model = candidateModel;

        ChatTransport transport = transportMap.get(spec.getKind().name());
        if (transport == null) {
            throw new AiValidationException("No transport available for provider kind: " + spec.getKind());
        }

        String systemInstruction = buildGateSystemPrompt(request.mode(), request.questionContext());
        long start = System.currentTimeMillis();

        String reply = transport.chat(spec, systemInstruction, request.messages(), model, apiKey);
        long latency = System.currentTimeMillis() - start;

        return new AiChatResponse(reply, providerId.toUpperCase(), model, latency);
    }

    public String generateRawCompletion(String providerId, String systemInstruction, String userPrompt, String modelOverride, String customApiKey) {
        ProviderSpec spec = providerRegistry.get(providerId.toLowerCase())
            .orElseThrow(() -> new AiValidationException("Unknown AI provider: " + providerId));

        String apiKey = providerRegistry.resolveApiKey(providerId.toLowerCase(), customApiKey);
        if (spec.isRequireKey() && (apiKey == null || apiKey.trim().isEmpty())) {
            throw new AiValidationException("Missing API key for provider: " + providerId);
        }

        String model = (modelOverride != null && !modelOverride.trim().isEmpty())
            ? modelOverride.trim()
            : providerRegistry.resolveDefaultModel(providerId.toLowerCase());

        ChatTransport transport = transportMap.get(spec.getKind().name());
        if (transport == null) {
            throw new AiValidationException("No transport for provider kind: " + spec.getKind());
        }

        List<ChatMessageDto> messages = List.of(new ChatMessageDto("user", userPrompt));
        return transport.chat(spec, systemInstruction, messages, model, apiKey);
    }

    private String buildGateSystemPrompt(String mode, QuestionContextDto ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are the GateLMS AI Studio Assistant, an elite Computer Science pedagogical mentor specializing in the Graduate Aptitude Test in Engineering (GATE CSE).
                Your mission is to guide students with mathematical rigor, conceptual clarity, and Socratic questioning.

                Guidelines:
                1. Always use standard LaTeX notation for mathematical equations, formulas, set theory, complexity notations ($O(n \\log n)$), and boolean algebra ($$...$$ for display math, $...$ for inline).
                2. When analyzing code or algorithms, provide formal loop invariants, time/space complexity derivations, and edge cases.
                """);

        if ("HINT".equalsIgnoreCase(mode)) {
            sb.append("3. SOCRATIC HINT MODE: NEVER give the final direct answer, numerical value, or correct option letter. Guide the student step by step by identifying the relevant theoretical invariant or asking a probing question.\n");
        } else if ("EXPLANATION".equalsIgnoreCase(mode)) {
            sb.append("3. FULL EXPLANATION MODE: Provide a step-by-step rigorous derivation, analyze why each incorrect option is false, and state the exact correct answer with proof.\n");
        }

        if (ctx != null) {
            sb.append("\n--- ACTIVE QUESTION CONTEXT ---\n");
            if (ctx.statement() != null) sb.append("Question: ").append(ctx.statement()).append("\n");
            if (ctx.type() != null) sb.append("Type: ").append(ctx.type()).append("\n");
            if (ctx.subjectName() != null) sb.append("Subject: ").append(ctx.subjectName()).append("\n");
            if (ctx.topicName() != null) sb.append("Topic: ").append(ctx.topicName()).append("\n");
            if (ctx.optionsJson() != null && !ctx.optionsJson().isEmpty()) sb.append("Options: ").append(ctx.optionsJson()).append("\n");
            sb.append("------------------------------\n");
        }

        return sb.toString();
    }
}
