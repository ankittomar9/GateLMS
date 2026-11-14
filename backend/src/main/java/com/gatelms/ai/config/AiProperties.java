package com.gatelms.ai.config;

import com.gatelms.ai.registry.ProviderKind;
import com.gatelms.ai.registry.ProviderSpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.*;

@Configuration
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
    private DefaultsConfig defaults = new DefaultsConfig();
    private ProvidersConfig providers = new ProvidersConfig();
    private WhisperConfig whisper = new WhisperConfig();
    private HttpConfig http = new HttpConfig();
    private QuotaConfig quota = new QuotaConfig();

    public DefaultsConfig getDefaults() { return defaults; }
    public void setDefaults(DefaultsConfig defaults) { this.defaults = defaults; }

    public ProvidersConfig getProviders() { return providers; }
    public void setProviders(ProvidersConfig providers) { this.providers = providers; }

    public WhisperConfig getWhisper() { return whisper; }
    public void setWhisper(WhisperConfig whisper) { this.whisper = whisper; }

    public HttpConfig getHttp() { return http; }
    public void setHttp(HttpConfig http) { this.http = http; }

    public QuotaConfig getQuota() { return quota; }
    public void setQuota(QuotaConfig quota) { this.quota = quota; }

    public static class DefaultsConfig {
        private String studyProvider = "GEMINI";
        public String getStudyProvider() { return studyProvider; }
        public void setStudyProvider(String studyProvider) { this.studyProvider = studyProvider; }
    }

    public static class ProvidersConfig {
        private LinkedHashMap<String, ProviderSpec> entries = defaultEntries();

        public LinkedHashMap<String, ProviderSpec> getEntries() { return entries; }
        public void setEntries(LinkedHashMap<String, ProviderSpec> entries) { this.entries = entries; }

        public ProviderSpec getGroq() {
            return entries.computeIfAbsent("groq", k -> defaultGroqSpec());
        }

        public ProviderSpec getGemini() {
            return entries.computeIfAbsent("gemini", k -> defaultGeminiSpec());
        }

        public ProviderSpec getOpenai() {
            return entries.computeIfAbsent("openai", k -> defaultOpenAiSpec());
        }

        public ProviderSpec getOllama() {
            return entries.computeIfAbsent("ollama", k -> defaultOllamaSpec());
        }

        public static ProviderSpec defaultGroqSpec() {
            return new ProviderSpec(
                ProviderKind.OPENAI_COMPATIBLE,
                "Groq (Llama 3.3 70B)",
                "https://api.groq.com/openai/v1",
                "",
                List.of("GROQ_API_KEY"),
                List.of("GROQ_DEFAULT_MODEL"),
                "llama-3.3-70b-versatile",
                null,
                "whisper-large-v3-turbo",
                List.of("llama-3.3-70b-versatile", "llama3-70b-8192", "llama-3.1-8b-instant", "whisper-large-v3-turbo"),
                List.of("llama", "whisper", "mixtral"),
                false,
                true,
                null
            );
        }

        public static ProviderSpec defaultGeminiSpec() {
            return new ProviderSpec(
                ProviderKind.GEMINI,
                "Google Gemini Flash",
                "https://generativelanguage.googleapis.com/v1beta/models",
                "",
                List.of("GEMINI_API_KEY", "GEMINI_KEY", "GOOGLE_API_KEY", "AI_STUDIO_API_KEY"),
                List.of("GEMINI_DEFAULT_MODEL"),
                "gemini-3.5-flash",
                "gemini-3.5-flash",
                null,
                List.of("gemini-3.8-flash", "gemini-3-flash-preview", "gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro", "gemini-flash"),
                List.of("gemini"),
                false,
                true,
                null
            );
        }

        public static ProviderSpec defaultOpenAiSpec() {
            return new ProviderSpec(
                ProviderKind.OPENAI_COMPATIBLE,
                "OpenAI",
                "https://api.openai.com/v1",
                "",
                List.of("OPENAI_API_KEY"),
                List.of("OPENAI_DEFAULT_MODEL"),
                "gpt-4o-mini",
                null,
                "whisper-1",
                List.of("gpt-4o-mini", "gpt-4o", "whisper-1"),
                List.of("gpt", "o", "whisper"),
                false,
                true,
                null
            );
        }

        public static ProviderSpec defaultOllamaSpec() {
            return new ProviderSpec(
                ProviderKind.OLLAMA,
                "Ollama (local)",
                "http://localhost:11434",
                "",
                Collections.emptyList(),
                List.of("OLLAMA_DEFAULT_MODEL"),
                "llama3",
                null,
                null,
                List.of("llama3.2", "llama3", "mistral", "gemma2"),
                Collections.emptyList(),
                true,
                false,
                0
            );
        }

        public static LinkedHashMap<String, ProviderSpec> defaultEntries() {
            LinkedHashMap<String, ProviderSpec> map = new LinkedHashMap<>();
            map.put("groq", defaultGroqSpec());
            map.put("gemini", defaultGeminiSpec());
            map.put("openai", defaultOpenAiSpec());
            map.put("ollama", defaultOllamaSpec());
            return map;
        }
    }

    public static class WhisperConfig {
        private String localEndpoint = "http://localhost:8178/inference";
        private String sttProvider = "auto";

        public String getLocalEndpoint() { return localEndpoint; }
        public void setLocalEndpoint(String localEndpoint) { this.localEndpoint = localEndpoint; }

        public String getSttProvider() { return sttProvider; }
        public void setSttProvider(String sttProvider) { this.sttProvider = sttProvider; }
    }

    public static class HttpConfig {
        private long connectTimeoutMs = 3000;
        private long chatReadTimeoutMs = 300000;
        private long sttReadTimeoutMs = 90000;
        private long statusReadTimeoutMs = 2000;
        private int maxRetries = 1;

        public long getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(long connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

        public long getChatReadTimeoutMs() { return chatReadTimeoutMs; }
        public void setChatReadTimeoutMs(long chatReadTimeoutMs) { this.chatReadTimeoutMs = chatReadTimeoutMs; }

        public long getSttReadTimeoutMs() { return sttReadTimeoutMs; }
        public void setSttReadTimeoutMs(long sttReadTimeoutMs) { this.sttReadTimeoutMs = sttReadTimeoutMs; }

        public long getStatusReadTimeoutMs() { return statusReadTimeoutMs; }
        public void setStatusReadTimeoutMs(long statusReadTimeoutMs) { this.statusReadTimeoutMs = statusReadTimeoutMs; }

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    }

    public static class QuotaConfig {
        private int chatPerHour = 20;
        private int transcribePer10min = 10;
        private int statusPerHour = 60;
        private int anatomyGeneratePerHour = 30;
        private int conceptGeneratePerHour = 10;
        private int primerGeneratePerHour = 10;

        public int getChatPerHour() { return chatPerHour; }
        public void setChatPerHour(int chatPerHour) { this.chatPerHour = chatPerHour; }

        public int getTranscribePer10min() { return transcribePer10min; }
        public void setTranscribePer10min(int transcribePer10min) { this.transcribePer10min = transcribePer10min; }

        public int getStatusPerHour() { return statusPerHour; }
        public void setStatusPerHour(int statusPerHour) { this.statusPerHour = statusPerHour; }

        public int getAnatomyGeneratePerHour() { return anatomyGeneratePerHour; }
        public void setAnatomyGeneratePerHour(int anatomyGeneratePerHour) { this.anatomyGeneratePerHour = anatomyGeneratePerHour; }

        public int getConceptGeneratePerHour() { return conceptGeneratePerHour; }
        public void setConceptGeneratePerHour(int conceptGeneratePerHour) { this.conceptGeneratePerHour = conceptGeneratePerHour; }

        public int getPrimerGeneratePerHour() { return primerGeneratePerHour; }
        public void setPrimerGeneratePerHour(int primerGeneratePerHour) { this.primerGeneratePerHour = primerGeneratePerHour; }
    }
}
