package harshal.temkar.ai.exception;

public class ValidationException extends AiException {
    
    public ValidationException(String details) {
    	
        super(ErrorCode.VALIDATION_ERROR, details);
    }
    
    public ValidationException(String details, Throwable cause) {
    	
        super(ErrorCode.VALIDATION_ERROR, details, cause);
    }
}