package harshal.temkar.ai.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	
	VALIDATION_ERROR("AI-1001", "Validation failed", HttpStatus.BAD_REQUEST),
	AI_SERVICE_ERROR("AI-2001", "AI service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
	RATE_LIMIT_EXCEEDED("AI-3001", "Rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
	TIMEOUT_ERROR("AI-4001", "Request timeout", HttpStatus.REQUEST_TIMEOUT),
	INTERNAL_ERROR("AI-5001", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
	PROMPT_TOO_LONG("AI-1002", "Prompt exceeds maximum length", HttpStatus.BAD_REQUEST),
	INVALID_MODEL("AI-1003", "Invalid model specified", HttpStatus.BAD_REQUEST),
	STREAMING_ERROR("AI-6001", "Streaming error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

	private final String code;
	private final String message;
	private final HttpStatus httpStatus;
}