package harshal.temkar.ai.conversation.repository;

import harshal.temkar.ai.conversation.model.ConversationContext;

import java.util.Optional;

public interface IConversationRepository {
    
    void save(String sessionId, ConversationContext context);
    
    Optional<ConversationContext> findBySessionId(String sessionId);
    
    void deleteBySessionId(String sessionId);
    
    boolean exists(String sessionId);
    
    void clear();
    
    String getCacheType();
}