package com.gatelms.ai.registry;

import com.gatelms.ai.config.AiProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ProviderRegistry {
    private final AiProperties properties;
    private final Environment env;

    public ProviderRegistry(AiProperties properties, Environment env) {
        this.properties = properties;
        this.env = env;
    }

    public Optional<ProviderSpec> get(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(properties.getProviders().getEntries().get(id.toLowerCase()));
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
        Optional<ProviderSpec> specOpt = get(providerId);
        if (specOpt.isEmpty()) return null;
        ProviderSpec spec = specOpt.get();

        if (spec.getApiKey() != null && !spec.getApiKey().trim().isEmpty()) {
            return spec.getApiKey().trim();
        }
        for (String envName : spec.getEnvKeyNames()) {
            String val = env.getProperty(envName);
            if (val != null && !val.trim().isEmpty()) {
                return val.trim();
            }
        }
        return null;
    }

    public String resolveDefaultModel(String providerId) {
        Optional<ProviderSpec> specOpt = get(providerId);
        if (specOpt.isEmpty()) return "";
        ProviderSpec spec = specOpt.get();

        for (String envName : spec.getEnvDefaultModelNames()) {
            String val = env.getProperty(envName);
            if (val != null && !val.trim().isEmpty()) {
                return val.trim();
            }
        }
        return spec.getDefaultModel();
    }

    public boolean isConfigured(String providerId) {
        Optional<ProviderSpec> specOpt = get(providerId);
        if (specOpt.isEmpty()) return false;
        ProviderSpec spec = specOpt.get();
        if (!spec.isRequireKey()) return true;
        String key = resolveApiKey(providerId, null);
        return key != null && !key.trim().isEmpty();
    }
}
