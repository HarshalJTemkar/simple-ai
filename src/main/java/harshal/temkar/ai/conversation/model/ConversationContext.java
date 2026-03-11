package harshal.temkar.ai.conversation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationContext implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String sessionId;
    private List<ConversationMessage> messages;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private Integer totalTokens;
    private Integer messageCount;
    
    public void addMessage(ConversationMessage message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);
        this.messageCount = this.messages.size();
        this.lastAccessedAt = LocalDateTime.now();
        
        if (message.getTokenCount() != null) {
            this.totalTokens = (this.totalTokens == null ? 0 : this.totalTokens) + message.getTokenCount();
        }
    }
    
    public List<ConversationMessage> getRecentMessages(int limit) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        
        int fromIndex = Math.max(0, messages.size() - limit);
        return new ArrayList<>(messages.subList(fromIndex, messages.size()));
    }
}