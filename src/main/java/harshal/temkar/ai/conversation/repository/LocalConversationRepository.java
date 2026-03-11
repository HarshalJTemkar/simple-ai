package harshal.temkar.ai.conversation.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import harshal.temkar.ai.config.ConversationCacheProperties;
import harshal.temkar.ai.conversation.model.ConversationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@ConditionalOnProperty(
    prefix = "app.conversation.cache",
    name = "type",
    havingValue = "local",
    matchIfMissing = true
)
public class LocalConversationRepository implements IConversationRepository {

    private final Cache<String, ConversationContext> cache;
    private final ConversationCacheProperties properties;

    public LocalConversationRepository(ConversationCacheProperties properties) {
        this.properties = properties;
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.getMaxSize())
                .expireAfterWrite(properties.getTtlMinutes(), TimeUnit.MINUTES)
                .recordStats()
                .build();
        
        log.info("Initialized Local Conversation Repository with max size: {}", properties.getMaxSize());
    }

    @Override
    public void save(String sessionId, ConversationContext context) {
        log.debug("Saving conversation to local cache: {}", sessionId);
        cache.put(sessionId, context);
    }

    @Override
    public Optional<ConversationContext> findBySessionId(String sessionId) {
        log.debug("Retrieving conversation from local cache: {}", sessionId);
        return Optional.ofNullable(cache.getIfPresent(sessionId));
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        log.debug("Deleting conversation from local cache: {}", sessionId);
        cache.invalidate(sessionId);
    }

    @Override
    public boolean exists(String sessionId) {
        return cache.getIfPresent(sessionId) != null;
    }

    @Override
    public void clear() {
        log.warn("Clearing all conversations from local cache");
        cache.invalidateAll();
    }

    @Override
    public String getCacheType() {
        return "local";
    }
}