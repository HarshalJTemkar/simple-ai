package harshal.temkar.ai.model.conversation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String messageId;
    private String role; // "user" or "assistant"
    private String content;
    private LocalDateTime timestamp;
    private Integer tokenCount;
}