package harshal.temkar.ai.model.chat;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import harshal.temkar.ai.util.TokenUsage;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {
	
	private String response;
	private String sessionId;
	private LocalDateTime timestamp;
	private TokenUsage usage; // Add token usage

	public ChatResponse(String response, String sessionId) {
		
		this.response = response;
		this.sessionId = sessionId;
		this.timestamp = LocalDateTime.now();
	}
	
	public ChatResponse(String response, String sessionId, TokenUsage usage) {
		
        this.response = response;
        this.sessionId = sessionId;
        this.usage = usage;
        this.timestamp = LocalDateTime.now();
    }
}