package harshal.temkar.ai.util;

import org.springframework.stereotype.Component;

import harshal.temkar.ai.config.ChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCounter {

    private final ChatProperties chatProperties;

    /**
     * Estimates token count using configurable character-based approximation
     * (see {@code app.chat.chars-per-token}).
     */
    public int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        int estimatedTokens = (int) Math.ceil(normalized.length() / chatProperties.getCharsPerToken());
        log.debug("Estimated {} tokens for text length: {}", estimatedTokens, text.length());
        return estimatedTokens;
    }

    /**
     * Count tokens for both request and response.
     */
    public TokenUsage calculateUsage(String requestText, String responseText) {
        int promptTokens = estimateTokenCount(requestText);
        int completionTokens = estimateTokenCount(responseText);
        int totalTokens = promptTokens + completionTokens;
        return new TokenUsage(promptTokens, completionTokens, totalTokens);
    }
}