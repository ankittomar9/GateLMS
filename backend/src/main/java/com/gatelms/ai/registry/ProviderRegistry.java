package com.gatelms.ai.registry;

import com.gatelms.ai.config.AiProperties;
import com.gatelms.ai.exception.AiExceptions.AiServiceUnavailableException;
import com.gatelms.ai.exception.AiExceptions.AiValidationException;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProviderRegistry {
    private final AiProperties properties;
    private final Environment env;

    public ProviderRegistry(AiProperties properties) {
        this(properties, null);
    }

    public ProviderRegistry(AiProperties properties, Environment env) {
        this.properties = properties;
        this.env = env;
    }

    @PostConstruct
    public void validate() {
        for (Map.Entry<String, ProviderSpec> entry : properties.getProviders().getEntries().entrySet()) {
            String id = entry.getKey();
            ProviderSpec spec = entry.getValue();
            if (spec.getDefaultModel() == null || spec.getDefaultModel().isBlank()) {
                throw new IllegalStateException("AI provider '" + id + "' configuration is invalid: defaultModel must not be blank");
            }
        }
    }

    public Optional<ProviderSpec> get(String id) {
        return Optional.ofNullable(find(id));
    }

    public ProviderSpec find(String id) {
        if (id == null) return null;
        String normalized = id.trim().toLowerCase();
        Map<String, ProviderSpec> entries = properties.getProviders().getEntries();
        if (entries.containsKey(normalized)) {
            return entries.get(normalized);
        }
        for (Map.Entry<String, ProviderSpec> e : entries.entrySet()) {
            if (e.getKey().equalsIgnoreCase(normalized)) {
                return e.getValue();
            }
        }
        return null;
    }

    public ProviderSpec require(String id) {
        ProviderSpec spec = find(id);
        if (spec == null) {
            Set<String> available = properties.getProviders().getEntries().keySet();
            throw new AiValidationException("Unknown AI provider '" + id + "'. Available providers: " + available);
        }
        return spec;
    }

    public List<String> listIds() {
        return new ArrayList<>(properties.getProviders().getEntries().keySet());
    }

    public Map<String, ProviderSpec> all() {
        return Collections.unmodifiableMap(properties.getProviders().getEntries());
    }

    public String resolveApiKey(String providerId, String userProvidedKey) {
        if (userProvidedKey != null && !userProvidedKey.trim().isEmpty()) {
            return userProvidedKey.trim();
        }
        ProviderSpec spec = find(providerId);
        if (spec == null) return null;

        if (spec.getApiKey() != null && !spec.getApiKey().trim().isEmpty()) {
            return spec.getApiKey().trim();
        }
        if (spec.getEnvKeyNames() != null) {
            for (String envName : spec.getEnvKeyNames()) {
                String val = env != null ? env.getProperty(envName) : System.getenv(envName);
                if (val != null && !val.trim().isEmpty()) {
                    return val.trim();
                }
            }
        }
        String defaultEnv = (providerId != null ? providerId.toUpperCase() : "") + "_API_KEY";
        String defVal = env != null ? env.getProperty(defaultEnv) : System.getenv(defaultEnv);
        if (defVal != null && !defVal.trim().isEmpty()) {
            return defVal.trim();
        }

        if (spec.isRequireKey()) {
            return null;
        }
        return "";
    }

    public String resolveDefaultModel(String providerId) {
        return resolveModel(providerId, null);
    }

    public String resolveModel(String id, String explicitModel) {
        if (explicitModel != null && !explicitModel.trim().isEmpty()) {
            return explicitModel.trim();
        }
        ProviderSpec spec = find(id);
        if (spec == null) return "";

        if (spec.getEnvDefaultModelNames() != null) {
            for (String envName : spec.getEnvDefaultModelNames()) {
                String val = env != null ? env.getProperty(envName) : System.getenv(envName);
                if (val != null && !val.trim().isEmpty()) {
                    return val.trim();
                }
            }
        }
        String defaultEnv = (id != null ? id.toUpperCase() : "") + "_DEFAULT_MODEL";
        String defVal = env != null ? env.getProperty(defaultEnv) : System.getenv(defaultEnv);
        if (defVal != null && !defVal.trim().isEmpty()) {
            return defVal.trim();
        }

        return spec.getDefaultModel() != null ? spec.getDefaultModel().trim() : "";
    }

    public boolean isModelAllowed(String provider, String model) {
        if (model == null || model.trim().isEmpty()) return false;
        String trimmed = model.trim();
        String upper = provider != null ? provider.trim().toUpperCase() : "";
        if ("LOCAL".equals(upper) || "WHISPER_LOCAL".equals(upper)) {
            List<String> localModels = List.of("ggml-large-v3-turbo-q5_0.bin", "ggml-base.en.bin", "whisper.cpp-large-v3-turbo");
            return localModels.stream().anyMatch(m -> m.equalsIgnoreCase(trimmed))
                    || trimmed.toLowerCase().contains("whisper")
                    || trimmed.endsWith(".bin");
        }
        ProviderSpec spec = find(provider);
        if (spec == null) return false;
        return spec.isModelAllowed(trimmed);
    }

    public String resolveStudyProvider(String explicit) {
        if (explicit != null && !explicit.trim().isEmpty()) {
            return explicit.trim();
        }
        String studyEnv = env != null ? env.getProperty("STUDY_PROVIDER") : System.getenv("STUDY_PROVIDER");
        if (studyEnv != null && !studyEnv.trim().isEmpty()) {
            return studyEnv.trim();
        }
        String def = properties.getDefaults() != null ? properties.getDefaults().getStudyProvider() : null;
        return (def != null && !def.isBlank()) ? def : "GEMINI";
    }

    public String resolveStudyProvider() {
        return resolveStudyProvider(null);
    }

    public boolean isConfigured(String providerId) {
        ProviderSpec spec = find(providerId);
        if (spec == null) return false;
        if (!spec.isRequireKey()) return true;
        String key = resolveApiKey(providerId, null);
        return key != null && !key.trim().isEmpty();
    }
}
