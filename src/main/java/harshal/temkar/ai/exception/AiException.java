package harshal.temkar.ai.exception;

import lombok.Getter;

@Getter
public class AiException extends RuntimeException {
	
	private final ErrorCode errorCode;
	private final String details;

	public AiException(ErrorCode errorCode) {
		
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.details = null;
	}

	public AiException(ErrorCode errorCode, String details) {
		
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.details = details;
	}

	public AiException(ErrorCode errorCode, String details, Throwable cause) {
		
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
		this.details = details;
	}
}