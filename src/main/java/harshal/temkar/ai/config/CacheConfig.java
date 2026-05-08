package harshal.temkar.ai.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

import harshal.temkar.ai.service.constants.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final ResponseCacheProperties responseCacheProperties;

    @Bean(Constants.CACHE_MANAGER_RESPONSE)
    CacheManager responseCacheManager() {
        log.info("Initializing Response Cache Manager");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                responseCacheProperties.getNames().toArray(new String[0]));

        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(responseCacheProperties.getMaxSize())
                .expireAfterWrite(responseCacheProperties.getTtlMinutes(), TimeUnit.MINUTES)
                .removalListener((key, value, cause) ->
                        log.debug("Removed cache entry: {}, cause: {}", key, cause));
        if (responseCacheProperties.isRecordStats()) {
            builder = builder.recordStats();
        }
        cacheManager.setCaffeine(builder);

        log.info("Response cache configured - MaxSize: {}, TTL: {} minutes, caches: {}",
                responseCacheProperties.getMaxSize(),
                responseCacheProperties.getTtlMinutes(),
                responseCacheProperties.getNames());
        return cacheManager;
    }
}