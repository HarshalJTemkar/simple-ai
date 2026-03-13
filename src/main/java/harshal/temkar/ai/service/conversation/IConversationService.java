package harshal.temkar.ai.service.conversation;

import java.util.List;
import java.util.Optional;

import harshal.temkar.ai.model.conversation.ConversationContext;
import harshal.temkar.ai.model.conversation.ConversationMessage;
import harshal.temkar.ai.model.conversation.ConversationSummary;

public interface IConversationService {
    
    void saveMessage(String sessionId, ConversationMessage message);
    
    Optional<ConversationContext> getContext(String sessionId);
    
    List<ConversationMessage> getRecentMessages(String sessionId, int limit);
    
    String buildPromptWithContext(String sessionId, String newMessage, int contextLimit);
    
    void deleteConversation(String sessionId);
    
    ConversationSummary getSummary(String sessionId);
    
    void clearAll();
}