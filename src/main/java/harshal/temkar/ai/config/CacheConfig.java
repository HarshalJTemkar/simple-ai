package harshal.temkar.ai.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean("responseCacheManager")
    CacheManager responseCacheManager() {
        log.info("Initializing Response Cache Manager");
        
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "chatResponses",
            "modelInfo",
            "systemPrompts"
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .recordStats()
                .removalListener((key, value, cause) -> 
                	log.debug("Removed conversation: {}, cause: {}", key, cause))
                );
        
        log.info("Response cache configured - MaxSize: 1000, TTL: 15 minutes");
        return cacheManager;
    }
}