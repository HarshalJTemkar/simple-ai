package harshal.temkar.ai.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import harshal.temkar.ai.service.constants.Constants;
import lombok.Data;

/**
 * Tunables for the in-process Caffeine-backed response cache used by
 * {@link harshal.temkar.ai.service.chat.ChatServiceImpl}.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.cache.response")
public class ResponseCacheProperties {

    private long maxSize = 1000L;
    private long ttlMinutes = 15L;
    private boolean recordStats = true;
    private List<String> names = List.of(
            Constants.CACHE_CHAT_RESPONSES,
            Constants.CACHE_MODEL_INFO,
            Constants.CACHE_SYSTEM_PROMPTS);
}
