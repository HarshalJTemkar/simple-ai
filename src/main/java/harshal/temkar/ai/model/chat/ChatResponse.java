package harshal.temkar.ai.model.chat;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {
	
	private String response;
	private String sessionId;
	private LocalDateTime timestamp;
	private Integer tokenCount; // For cost tracking
	private String model;

	public ChatResponse(String response, String sessionId) {
		
		this.response = response;
		this.sessionId = sessionId;
		this.timestamp = LocalDateTime.now();
	}
}