package harshal.temkar.ai.config;

import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(
    prefix = "app.conversation.cache",
    name = "type",
    havingValue = "local",
    matchIfMissing = true
)
@RequiredArgsConstructor
public class LocalCacheConfig {

    private final ConversationCacheProperties properties;

    @Bean
    CacheManager conversationCacheManager() {
        log.info("Initializing Local Caffeine Cache for conversations");
        log.info("Cache settings - MaxSize: {}, TTL: {} minutes", 
                 properties.getMaxSize(), properties.getTtlMinutes());
        
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("conversations");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(properties.getMaxSize())
                .expireAfterWrite(properties.getTtlMinutes(), TimeUnit.MINUTES)
                .recordStats());
        
        return cacheManager;
    }
}