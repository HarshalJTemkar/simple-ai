package harshal.temkar.ai.exception;

public class StreamingException extends AiException {
    
    public StreamingException(String details) {
        super(ErrorCode.STREAMING_ERROR, details);
    }
    
    public StreamingException(String details, Throwable cause) {
        super(ErrorCode.STREAMING_ERROR, details, cause);
    }
}