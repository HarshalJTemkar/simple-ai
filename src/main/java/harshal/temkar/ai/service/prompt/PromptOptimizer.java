package harshal.temkar.ai.service.prompt;

import org.springframework.stereotype.Component;

import harshal.temkar.ai.config.PromptProperties;
import harshal.temkar.ai.model.chat.OptimizedPrompt;
import harshal.temkar.ai.util.TokenCounter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptOptimizer {

    private final PromptProperties promptProperties;
    private final TokenCounter tokenCounter;

    public OptimizedPrompt optimize(String systemPrompt, String userPrompt) {
        if (!promptProperties.getOptimization().isEnabled()) {
            return createUnoptimized(systemPrompt, userPrompt);
        }

        log.debug("Optimizing prompt - System: {} chars, User: {} chars", 
                  systemPrompt != null ? systemPrompt.length() : 0, 
                  userPrompt.length());

        String optimizedSystem = optimizeSystemPrompt(systemPrompt);
        String optimizedUser = optimizeUserPrompt(userPrompt);
        String fullPrompt = buildFullPrompt(optimizedSystem, optimizedUser);
        
        int estimatedTokens = tokenCounter.estimateTokenCount(fullPrompt);
        
        return OptimizedPrompt.builder()
                .systemPrompt(optimizedSystem)
                .userPrompt(optimizedUser)
                .fullPrompt(fullPrompt)
                .estimatedTokens(estimatedTokens)
                .optimized(true)
                .optimizationNotes("Optimized for token efficiency")
                .build();
    }

    private String optimizeSystemPrompt(String systemPrompt) {
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            return "";
        }

        String optimized = systemPrompt;

        if (promptProperties.getOptimization().isTrimWhitespace()) {
            optimized = optimized.trim();
        }

        if (promptProperties.getOptimization().isRemoveEmptyLines()) {
            optimized = optimized.replaceAll("(?m)^[ \t]*\r?\n", "");
        }

        // Enforce max system prompt length
        Integer maxLength = promptProperties.getOptimization().getMaxSystemPromptLength();
        if (optimized.length() > maxLength) {
            log.warn("System prompt truncated from {} to {} characters", 
                     optimized.length(), maxLength);
            optimized = optimized.substring(0, maxLength) + "...";
        }

        return optimized;
    }

    private String optimizeUserPrompt(String userPrompt) {
        if (userPrompt == null || userPrompt.isEmpty()) {
            return "";
        }

        String optimized = userPrompt;

        if (promptProperties.getOptimization().isTrimWhitespace()) {
            optimized = optimized.trim();
        }

        return optimized;
    }

    private String buildFullPrompt(String systemPrompt, String userPrompt) {
        StringBuilder full = new StringBuilder();
        
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            full.append(systemPrompt).append("\n\n");
        }
        
        full.append(userPrompt);
        
        return full.toString();
    }

    private OptimizedPrompt createUnoptimized(String systemPrompt, String userPrompt) {
        String fullPrompt = buildFullPrompt(systemPrompt, userPrompt);
        
        return OptimizedPrompt.builder()
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .fullPrompt(fullPrompt)
                .estimatedTokens(tokenCounter.estimateTokenCount(fullPrompt))
                .optimized(false)
                .optimizationNotes("Optimization disabled")
                .build();
    }
}