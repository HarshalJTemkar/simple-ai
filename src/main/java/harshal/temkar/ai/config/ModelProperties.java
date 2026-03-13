package harshal.temkar.ai.config;

import harshal.temkar.ai.model.chat.AIModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai.models")
public class ModelProperties {
    
    // Changed: Single AIModel instead of separate provider + model
    private AIModel defaultProviderModel = AIModel.OLLAMA_LLAMA3; // Default
    
    private Map<String, ModelConfig> providers = new HashMap<>();
    
    @Data
    public static class ModelConfig {
        private boolean enabled;
        private Double defaultTemperature;
        private Integer maxTokens;
        private Integer timeout; // milliseconds
    }
    
    // Helper methods for backward compatibility
    public String getDefaultProvider() {
        return defaultProviderModel.getProviderName();
    }
    
    public String getDefaultModel() {
        return defaultProviderModel.getModelName();
    }
}