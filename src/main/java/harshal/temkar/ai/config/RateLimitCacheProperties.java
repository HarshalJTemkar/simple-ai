package harshal.temkar.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Tunables for the per-IP rate limiter cache (separate from limit values themselves).
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit.cache")
public class RateLimitCacheProperties {

    /** Maximum number of distinct client buckets retained in memory. */
    private long maxSize = 100_000L;
}
