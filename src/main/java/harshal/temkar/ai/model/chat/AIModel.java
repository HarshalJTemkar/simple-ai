package harshal.temkar.ai.model.chat;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum AIModel {
    // Ollama models - composed from provider-specific enums
    OLLAMA_LLAMA3(AIProvider.OLLAMA, OllamaModel.LLAMA3),
    OLLAMA_QWEN(AIProvider.OLLAMA, OllamaModel.QWEN),
    
    // Anthropic models
    ANTHROPIC_CLAUDE_3_5_SONNET(AIProvider.ANTHROPIC, AnthropicModel.CLAUDE_3_5_SONNET);
    
    private final AIProvider provider;
    private final Object providerModel; // Generic holder for provider-specific model enum
    
    AIModel(AIProvider provider, Object providerModel) {
        this.provider = provider;
        this.providerModel = providerModel;
    }
    
    // ==================== GETTERS ====================
    
    public String getProviderName() {
        return provider.getProviderName();
    }
    
    public String getModelName() {
        if (providerModel instanceof OllamaModel) {
            return ((OllamaModel) providerModel).getModelName();
        } else if (providerModel instanceof AnthropicModel) {
            return ((AnthropicModel) providerModel).getModelName();
        }
        return null;
    }
    
    // ==================== FACTORY METHODS ====================
    /**
     * Factory method: Create AIModel from OllamaModel
     */
    public static AIModel fromOllama(OllamaModel model) {
        return Arrays.stream(values())
                .filter(aiModel -> aiModel.provider == AIProvider.OLLAMA)
                .filter(aiModel -> aiModel.providerModel == model)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "No AIModel found for OllamaModel: " + model));
    }
    
    /**
     * Factory method: Create AIModel from AnthropicModel
     */
    public static AIModel fromAnthropic(AnthropicModel model) {
        return Arrays.stream(values())
                .filter(aiModel -> aiModel.provider == AIProvider.ANTHROPIC)
                .filter(aiModel -> aiModel.providerModel == model)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "No AIModel found for AnthropicModel: " + model));
    }
    
    /**
     * Factory method: Create AIModel from provider name and model name
     */
    public static Optional<AIModel> fromProviderAndModel(String providerName, String modelName) {
        return Arrays.stream(values())
                .filter(aiModel -> aiModel.getProviderName().equalsIgnoreCase(providerName))
                .filter(aiModel -> aiModel.getModelName().equalsIgnoreCase(modelName))
                .findFirst();
    }
    
    /**
     * Factory method: Create AIModel from combined string (e.g., "ollama:llama3")
     */
    public static Optional<AIModel> fromString(String combinedModel) {
        if (combinedModel == null || !combinedModel.contains(":")) {
            return Optional.empty();
        }
        
        String[] parts = combinedModel.split(":");
        if (parts.length != 2) {
            return Optional.empty();
        }
        
        return fromProviderAndModel(parts[0], parts[1]);
    }
    
    /**
     * Get all models for a specific provider
     */
    public static AIModel[] getModelsForProvider(AIProvider provider) {
        return Arrays.stream(values())
                .filter(aiModel -> aiModel.provider == provider)
                .toArray(AIModel[]::new);
    }
    
    /**
     * Check if a provider has a specific model
     */
    public static boolean isModelAvailable(AIProvider provider, String modelName) {
        return Arrays.stream(values())
                .anyMatch(aiModel -> aiModel.provider == provider && 
                         aiModel.getModelName().equalsIgnoreCase(modelName));
    }
    
    /**
     * Get provider-specific model enum
     */
    public <T> T getProviderModelAs(Class<T> type) {
        if (type.isInstance(providerModel)) {
            return type.cast(providerModel);
        }
        throw new ClassCastException("Provider model is not of type " + type.getName());
    }
}