package harshal.temkar.ai.controller.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import harshal.temkar.ai.config.ModelProperties;
import harshal.temkar.ai.model.chat.AIModel;
import harshal.temkar.ai.model.chat.ModelInfo;
import harshal.temkar.ai.service.chat.ModelSelector;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
@Tag(name = "Model Management", description = "AI Model provider information")
public class ModelController {

    private final ModelSelector modelSelector;
    private final ModelProperties modelProperties;

    @GetMapping("/providers")
    @Operation(summary = "List available providers")
    public ResponseEntity<Set<String>> getProviders() {
        log.debug("Fetching available providers");
        return ResponseEntity.ok(modelSelector.getAvailableProviders());
    }

    @GetMapping("/providers/{provider}/available")
    @Operation(summary = "Check provider availability")
    public ResponseEntity<Boolean> isProviderAvailable(@PathVariable String provider) {
        log.debug("Checking availability for provider: {}", provider);
        return ResponseEntity.ok(modelSelector.isProviderAvailable(provider));
    }

    @GetMapping("/providers/info")
    @Operation(
        summary = "Get all model information", 
        description = "Get all AIModel enums with provider, model, enabled status, and accessibility by actually testing each model"
    )
    public ResponseEntity<List<ModelInfo>> getAllModelsInfo() {
        log.info("Fetching all models information from AIModel enum");
        
        List<ModelInfo> modelInfoList = new ArrayList<>();
        
        for (AIModel aiModel : AIModel.values()) {
            String providerName = aiModel.getProviderName();
            String modelName = aiModel.getModelName();
            
            ModelProperties.ModelConfig providerConfig = 
                modelProperties.getProviders().get(providerName);
            
            boolean enabled = false;
            Double defaultTemperature = 0.7;
            Long maxTokens = 2048L;
            
            if (providerConfig != null) {
                enabled = providerConfig.isEnabled();
                defaultTemperature = providerConfig.getDefaultTemperature();
                maxTokens = providerConfig.getMaxTokens() != null 
                    ? providerConfig.getMaxTokens().longValue() 
                    : 2048L;
            }
            
            boolean accessible = testSpecificModelAccessibility(aiModel);
            
            ModelInfo modelInfo = ModelInfo.builder()
                    .provider(providerName)
                    .model(modelName)
                    .enabled(enabled)
                    .accessible(accessible)
                    .defaultTemperature(defaultTemperature)
                    .maxTokens(maxTokens)
                    .build();
            
            modelInfoList.add(modelInfo);
            
            log.debug("Model: {}, Provider: {}, Enabled: {}, Accessible: {}", 
                      modelName, providerName, enabled, accessible);
        }
        
        log.info("Fetched {} models information", modelInfoList.size());
        return ResponseEntity.ok(modelInfoList);
    }
    
    private boolean testSpecificModelAccessibility(AIModel aiModel) {
        try {
            // First check if provider is available
            ChatClient chatClient = modelSelector.selectClient(aiModel.getProviderName());
            
            // Actually test the specific model with a minimal prompt
            String testPrompt = "Hi";
            
            chatClient.prompt()
                    .user(testPrompt)
                    .options(ChatOptions.builder()
                            .model(aiModel.getModelName()) // Test specific model
                            .maxTokens(5) // Minimal tokens to reduce load
                            .build())
                    .call()
                    .content();
            
            log.debug("Model {} ({}) is accessible", aiModel.getModelName(), aiModel.getProviderName());
            return true;
            
        } catch (Exception e) {
            log.debug("Model {} ({}) is NOT accessible: {}", 
                      aiModel.getModelName(), aiModel.getProviderName(), e.getMessage());
            return false;
        }
    }
}