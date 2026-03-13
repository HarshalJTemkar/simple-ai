package harshal.temkar.ai.model.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummary {
    private String sessionId;
    private Integer messageCount;
    private Integer totalTokens;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private String cacheType; // "local", "redis", or "dynamodb"
}