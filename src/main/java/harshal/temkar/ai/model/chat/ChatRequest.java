package harshal.temkar.ai.model.chat;

import javax.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    
    @NotBlank(message = "Message cannot be blank")
    @Size(min = 1, 
    		max = 4000, 
    		message = "Message must be between 1 and 4000 characters")
    private String message;
    
    @Size(max = 100, 
    		message = "Session ID cannot exceed 100 characters")
    private String sessionId; // For conversation context
    
    private String model; // Optional model override
    
    private Double temperature; // Optional temperature override
    
    private Boolean stream; // Enable streaming mode (default: false)
    
    private Integer maxTokens; // Maximum tokens to generate
}