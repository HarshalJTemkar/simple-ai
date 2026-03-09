package harshal.temkar.ai.model.chat;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import harshal.temkar.ai.util.TokenUsage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamingChatResponse {
    
    private String id; // Unique message ID
    private String content; // Token content
    private String sessionId;
    private Boolean done; // Indicates if streaming is complete
    private LocalDateTime timestamp;
    private Integer currentTokens; // Current chunk token count
    private TokenUsage usage; // Final usage (only when done=true)
    
    // For error handling
    private String error;
    private String errorCode;
}