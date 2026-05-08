package harshal.temkar.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Tunables for the chat layer (history depth, citation excerpt sizing, etc.).
 * Everything is overridable via {@code app.chat.*}.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.chat")
public class ChatProperties {

    /** Number of recent conversation messages to inject as context for follow-up turns. */
    private int historyContextLimit = 5;

    /** Max characters of a chunk shown as a citation excerpt before truncation. */
    private int excerptMaxLength = 200;

    /** Suffix appended when an excerpt is truncated. */
    private String excerptTruncationSuffix = "...";

    /** Hard system-prompt fallback used if no template / role-specific prompt is available. */
    private String defaultSystemPromptFallback =
            "You are a helpful AI assistant. Provide accurate, concise, and helpful responses.";

    /** Approximate characters per LLM token (used for offline/heuristic token counting). */
    private double charsPerToken = 4.0d;

    /** Default user-facing label for an unknown filename. */
    private String unknownFilenameLabel = "Unknown";
}
