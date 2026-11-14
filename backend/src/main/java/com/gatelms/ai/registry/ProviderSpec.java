package com.gatelms.ai.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProviderSpec {
    private ProviderKind kind = ProviderKind.OPENAI_COMPATIBLE;
    private String label = "";
    private String endpoint = "";
    private String apiKey = "";
    private List<String> envKeyNames = new ArrayList<>();
    private List<String> envDefaultModelNames = new ArrayList<>();
    private String defaultModel = "";
    private String fallbackModel;
    private String sttModel;
    private List<String> modelAllowlist = new ArrayList<>();
    private List<String> modelPrefixes = new ArrayList<>();
    private boolean allowAnyModel = false;
    private boolean requireKey = true;
    private Integer maxRetriesOverride;

    public ProviderSpec() {}

    public ProviderSpec(
        ProviderKind kind,
        String label,
        String endpoint,
        String apiKey,
        List<String> envKeyNames,
        List<String> envDefaultModelNames,
        String defaultModel,
        String fallbackModel,
        String sttModel,
        List<String> modelAllowlist,
        List<String> modelPrefixes,
        boolean allowAnyModel,
        boolean requireKey,
        Integer maxRetriesOverride
    ) {
        this.kind = kind;
        this.label = label != null ? label : "";
        this.endpoint = endpoint != null ? endpoint : "";
        this.apiKey = apiKey != null ? apiKey : "";
        this.envKeyNames = envKeyNames != null ? envKeyNames : new ArrayList<>();
        this.envDefaultModelNames = envDefaultModelNames != null ? envDefaultModelNames : new ArrayList<>();
        this.defaultModel = defaultModel != null ? defaultModel : "";
        this.fallbackModel = fallbackModel;
        this.sttModel = sttModel;
        this.modelAllowlist = modelAllowlist != null ? modelAllowlist : new ArrayList<>();
        this.modelPrefixes = modelPrefixes != null ? modelPrefixes : new ArrayList<>();
        this.allowAnyModel = allowAnyModel;
        this.requireKey = requireKey;
        this.maxRetriesOverride = maxRetriesOverride;
    }

    public ProviderKind getKind() { return kind; }
    public void setKind(ProviderKind kind) { this.kind = kind; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public List<String> getEnvKeyNames() { return envKeyNames; }
    public void setEnvKeyNames(List<String> envKeyNames) { this.envKeyNames = envKeyNames; }

    public List<String> getEnvDefaultModelNames() { return envDefaultModelNames; }
    public void setEnvDefaultModelNames(List<String> envDefaultModelNames) { this.envDefaultModelNames = envDefaultModelNames; }

    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }

    public String getFallbackModel() { return fallbackModel; }
    public void setFallbackModel(String fallbackModel) { this.fallbackModel = fallbackModel; }

    public String getSttModel() { return sttModel; }
    public void setSttModel(String sttModel) { this.sttModel = sttModel; }

    public List<String> getModelAllowlist() { return modelAllowlist; }
    public void setModelAllowlist(List<String> modelAllowlist) { this.modelAllowlist = modelAllowlist; }

    public List<String> getModelPrefixes() { return modelPrefixes; }
    public void setModelPrefixes(List<String> modelPrefixes) { this.modelPrefixes = modelPrefixes; }

    public boolean isAllowAnyModel() { return allowAnyModel; }
    public void setAllowAnyModel(boolean allowAnyModel) { this.allowAnyModel = allowAnyModel; }

    public boolean isRequireKey() { return requireKey; }
    public void setRequireKey(boolean requireKey) { this.requireKey = requireKey; }

    public Integer getMaxRetriesOverride() { return maxRetriesOverride; }
    public void setMaxRetriesOverride(Integer maxRetriesOverride) { this.maxRetriesOverride = maxRetriesOverride; }
}
