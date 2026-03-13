package harshal.temkar.ai.repository.conversation;

import java.util.Optional;

import harshal.temkar.ai.model.conversation.ConversationContext;

public interface IConversationRepository {
    
    void save(String sessionId, ConversationContext context);
    
    Optional<ConversationContext> findBySessionId(String sessionId);
    
    void deleteBySessionId(String sessionId);
    
    boolean exists(String sessionId);
    
    void clear();
}