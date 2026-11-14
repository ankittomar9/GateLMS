package com.gatelms.ai.service;

import com.gatelms.ai.config.AiProperties;
import com.gatelms.ai.dto.AiDtos.ProviderStatusDto;
import com.gatelms.ai.registry.ProviderRegistry;
import com.gatelms.ai.registry.ProviderSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.*;

@Service
public class AiStatusService {
    private static final Logger log = LoggerFactory.getLogger(AiStatusService.class);
    private final ProviderRegistry providerRegistry;
    private final AiProperties aiProperties;
    private final RestClient statusProbeClient;

    public AiStatusService(ProviderRegistry providerRegistry, AiProperties aiProperties, RestClient.Builder restClientBuilder) {
        this.providerRegistry = providerRegistry;
        this.aiProperties = aiProperties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1500);
        factory.setReadTimeout((int) aiProperties.getHttp().getStatusReadTimeoutMs());
        this.statusProbeClient = restClientBuilder.requestFactory(factory).build();
    }

    public List<ProviderStatusDto> getStatuses(Map<String, String> byokMap) {
        List<ProviderStatusDto> results = new ArrayList<>();
        for (String id : providerRegistry.listIds()) {
            results.add(checkProvider(id, byokMap.get(id)));
        }
        return results;
    }

    private ProviderStatusDto checkProvider(String id, String customKey) {
        Optional<ProviderSpec> specOpt = providerRegistry.get(id);
        if (specOpt.isEmpty()) {
            return new ProviderStatusDto(id.toUpperCase(), false, "NOT_CONFIGURED", null, "Provider not found", id);
        }
        ProviderSpec spec = specOpt.get();
        String apiKey = providerRegistry.resolveApiKey(id, customKey);
        boolean isConfigured = !spec.isRequireKey() || (apiKey != null && !apiKey.trim().isEmpty());

        if (!isConfigured) {
            return new ProviderStatusDto(id.toUpperCase(), false, "NOT_CONFIGURED", spec.getDefaultModel(), "API Key not configured", spec.getLabel());
        }

        if ("ollama".equalsIgnoreCase(id)) {
            try {
                String ep = spec.getEndpoint().replaceAll("/+$", "") + "/api/tags";
                statusProbeClient.get().uri(ep).retrieve().toBodilessEntity();
                return new ProviderStatusDto(id.toUpperCase(), true, "READY", spec.getDefaultModel(), null, spec.getLabel());
            } catch (Exception e) {
                return new ProviderStatusDto(id.toUpperCase(), true, "UNREACHABLE", spec.getDefaultModel(), "Local Ollama daemon not reachable", spec.getLabel());
            }
        }

        return new ProviderStatusDto(id.toUpperCase(), true, "READY", spec.getDefaultModel(), null, spec.getLabel());
    }
}
