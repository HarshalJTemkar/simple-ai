package harshal.temkar.ai.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
	
    private String errorCode;
    private String message;
    private String details;
    private LocalDateTime timestamp;
    private String path;
    private String correlationId;
    private Map<String, String> validationErrors;
}