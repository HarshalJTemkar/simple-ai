package harshal.temkar.ai.conversation.service;

import harshal.temkar.ai.conversation.model.ConversationContext;
import harshal.temkar.ai.conversation.model.ConversationMessage;
import harshal.temkar.ai.conversation.model.ConversationSummary;

import java.util.List;
import java.util.Optional;

public interface IConversationService {
    
    void saveMessage(String sessionId, ConversationMessage message);
    
    Optional<ConversationContext> getContext(String sessionId);
    
    List<ConversationMessage> getRecentMessages(String sessionId, int limit);
    
    String buildPromptWithContext(String sessionId, String newMessage, int contextLimit);
    
    void deleteConversation(String sessionId);
    
    ConversationSummary getSummary(String sessionId);
    
    void clearAll();
}