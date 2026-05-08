package harshal.temkar.ai.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Per-IP rate limiting configuration. A simple token-bucket implementation
 * backed by Caffeine is applied to the configured path patterns.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /** Whether rate limiting is enabled at all. */
    private boolean enabled = true;

    /** Tokens allowed per window per client (IP). */
    private int capacity = 60;

    /** Window length, in seconds. */
    private int windowSeconds = 60;

    /** Path patterns the limiter applies to. */
    private List<String> paths = List.of("/api/v1/chat/**", "/api/v1/documents/ask", "/api/v1/documents/upload");
}
