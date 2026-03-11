package harshal.temkar.ai.conversation.repository;

import harshal.temkar.ai.config.ConversationCacheProperties;
import harshal.temkar.ai.conversation.model.ConversationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Repository
@ConditionalOnProperty(
    prefix = "app.conversation.cache",
    name = "type",
    havingValue = "redis"
)
@RequiredArgsConstructor
public class RedisConversationRepository implements IConversationRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConversationCacheProperties properties;

    private String buildKey(String sessionId) {
        return properties.getRedisKeyPrefix() + sessionId;
    }

    @Override
    public void save(String sessionId, ConversationContext context) {
        String key = buildKey(sessionId);
        log.debug("Saving conversation to Redis: {}", key);
        
        redisTemplate.opsForValue().set(
            key, 
            context, 
            Duration.ofMinutes(properties.getTtlMinutes())
        );
    }

    @Override
    public Optional<ConversationContext> findBySessionId(String sessionId) {
        String key = buildKey(sessionId);
        log.debug("Retrieving conversation from Redis: {}", key);
        
        Object value = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(value)
                .map(v -> (ConversationContext) v);
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        String key = buildKey(sessionId);
        log.debug("Deleting conversation from Redis: {}", key);
        redisTemplate.delete(key);
    }

    @Override
    public boolean exists(String sessionId) {
        String key = buildKey(sessionId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void clear() {
        log.warn("Clearing all conversations from Redis");
        redisTemplate.keys(properties.getRedisKeyPrefix() + "*")
                .forEach(redisTemplate::delete);
    }

    @Override
    public String getCacheType() {
        return "redis";
    }
}