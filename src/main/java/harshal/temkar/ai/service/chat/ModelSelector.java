package harshal.temkar.ai.service.chat;

import harshal.temkar.ai.config.ModelProperties;
import harshal.temkar.ai.exception.AiException;
import harshal.temkar.ai.exception.ErrorCode;
import harshal.temkar.ai.model.chat.AIModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelSelector {

    private final Map<String, ChatClient> chatClientRegistry;
    private final ModelProperties modelProperties;

    /**
     * Select ChatClient based on AIModel
     */
    public ChatClient selectClient(AIModel model) {
        if (model == null) {
            model = modelProperties.getDefaultProviderModel();
            log.debug("No model specified, using default: {}", model);
        }
        
        String provider = model.getProviderName();
        ChatClient chatClient = chatClientRegistry.get(provider.toLowerCase());
        
        if (chatClient == null) {
            log.error("Provider '{}' not found in registry. Available providers: {}", 
                      provider, chatClientRegistry.keySet());
            throw new AiException(ErrorCode.INVALID_MODEL, 
                "Provider '" + provider + "' is not configured or available");
        }
        
        log.debug("Selected ChatClient for provider: {}, model: {}", 
                  provider, model.getModelName());
        return chatClient;
    }

    /**
     * Select ChatClient based on provider name (backward compatibility)
     */
    public ChatClient selectClient(String provider) {
        if (provider == null || provider.isBlank()) {
            return getDefaultClient();
        }
        
        ChatClient chatClient = chatClientRegistry.get(provider.toLowerCase());
        
        if (chatClient == null) {
            log.error("Provider '{}' not found in registry. Available providers: {}", 
                      provider, chatClientRegistry.keySet());
            throw new AiException(ErrorCode.INVALID_MODEL, 
                "Provider '" + provider + "' is not configured or available");
        }
        
        log.debug("Selected ChatClient for provider: {}", provider);
        return chatClient;
    }

    /**
     * Get default ChatClient based on configured default AIModel
     */
    public ChatClient getDefaultClient() {
        AIModel defaultModel = modelProperties.getDefaultProviderModel();
        log.debug("Using default model: {}", defaultModel);
        return selectClient(defaultModel);
    }
    
    /**
     * Get default AIModel
     */
    public AIModel getDefaultModel() {
        return modelProperties.getDefaultProviderModel();
    }

    /**
     * Check if provider is available
     */
    public boolean isProviderAvailable(String provider) {
        return chatClientRegistry.containsKey(provider.toLowerCase());
    }

    /**
     * Get all available providers
     */
    public Set<String> getAvailableProviders() {
        return chatClientRegistry.keySet();
    }
}