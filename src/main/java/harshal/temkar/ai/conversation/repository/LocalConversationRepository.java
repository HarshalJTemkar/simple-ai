package harshal.temkar.ai.conversation.repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import harshal.temkar.ai.config.ConversationCacheProperties;
import harshal.temkar.ai.model.conversation.ConversationContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class LocalConversationRepository implements IConversationRepository {

    private final Cache<String, ConversationContext> cache;

    public LocalConversationRepository(ConversationCacheProperties properties) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.getMaxSize())
                .expireAfterWrite(properties.getTtlMinutes(), TimeUnit.MINUTES)
                .recordStats()
                .build();
        
        log.info("Initialized Local Conversation Repository - MaxSize: {}, TTL: {} minutes", 
                 properties.getMaxSize(), properties.getTtlMinutes());
    }

    @Override
    public void save(String sessionId, ConversationContext context) {
        log.debug("Saving conversation: {}", sessionId);
        cache.put(sessionId, context);
    }

    @Override
    public Optional<ConversationContext> findBySessionId(String sessionId) {
        log.debug("Retrieving conversation: {}", sessionId);
        return Optional.ofNullable(cache.getIfPresent(sessionId));
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        log.debug("Deleting conversation: {}", sessionId);
        cache.invalidate(sessionId);
    }

    @Override
    public boolean exists(String sessionId) {
        return cache.getIfPresent(sessionId) != null;
    }

    @Override
    public void clear() {
        log.warn("Clearing all conversations");
        cache.invalidateAll();
    }
}