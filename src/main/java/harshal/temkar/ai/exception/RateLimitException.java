package harshal.temkar.ai.exception;

public class RateLimitException extends AiException {
    
    public RateLimitException(String details) {
    	
        super(ErrorCode.RATE_LIMIT_EXCEEDED, details);
    }
    
    public RateLimitException(String details, Throwable cause) {
    	
        super(ErrorCode.RATE_LIMIT_EXCEEDED, details, cause);
    }
}