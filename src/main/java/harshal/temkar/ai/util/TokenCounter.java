package harshal.temkar.ai.util;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TokenCounter {

    /**
     * Estimates token count using character-based approximation
     * GPT-style: ~4 characters per token
     * Llama-style: ~3.5 characters per token
     */
    public int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // Remove extra whitespace and calculate
        String normalized = text.replaceAll("\\s+", " ").trim();
        
        // Average: 4 characters per token
        int estimatedTokens = (int) Math.ceil(normalized.length() / 4.0);
        
        log.debug("Estimated {} tokens for text length: {}", estimatedTokens, text.length());
        
        return estimatedTokens;
    }
    
    /**
     * Count tokens for both request and response
     */
    public TokenUsage calculateUsage(String requestText, String responseText) {
        int promptTokens = estimateTokenCount(requestText);
        int completionTokens = estimateTokenCount(responseText);
        int totalTokens = promptTokens + completionTokens;
        
        return new TokenUsage(promptTokens, completionTokens, totalTokens);
    }
}