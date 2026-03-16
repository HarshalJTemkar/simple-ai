package harshal.temkar.ai.model.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptContext {
	
    private PromptRole role;
    private String systemPrompt;
    private String userMessage;
    private String conversationHistory;
    private Map<String, Object> variables;
    private Integer maxLength;
}