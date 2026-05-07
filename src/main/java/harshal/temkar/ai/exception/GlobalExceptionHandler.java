package harshal.temkar.ai.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AiException.class)
    public ResponseEntity<ErrorResponse> handleAiException(
    		AiException ex, 
    		HttpServletRequest request) {
    	
        String correlationId = getCorrelationId();
        
        log.error("AiException occurred. CorrelationId: {}, ErrorCode: {}, Details: {}", 
                  correlationId, ex.getErrorCode().getCode(), ex.getDetails(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode().getCode())
                .message(ex.getErrorCode().getMessage())
                .details(ex.getDetails())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, 
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        Map<String, String> validationErrors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        log.error("Validation failed. CorrelationId: {}, Errors: {}", correlationId, validationErrors);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCode.VALIDATION_ERROR.getCode())
                .message(ErrorCode.VALIDATION_ERROR.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        String correlationId = getCorrelationId();
        Map<String, String> validationErrors = new HashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String path = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "param";
            // strip method prefix e.g. "uploadDocument.metadata" -> "metadata"
            int dot = path.lastIndexOf('.');
            String field = dot >= 0 ? path.substring(dot + 1) : path;
            validationErrors.put(field, v.getMessage());
        }

        log.error("Constraint violation. CorrelationId: {}, Errors: {}", correlationId, validationErrors);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCode.VALIDATION_ERROR.getCode())
                .message(ErrorCode.VALIDATION_ERROR.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        String correlationId = getCorrelationId();
        log.warn("Upload too large. CorrelationId: {}, Details: {}", correlationId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCode.FILE_TOO_LARGE.getCode())
                .message(ErrorCode.FILE_TOO_LARGE.getMessage())
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(ErrorCode.FILE_TOO_LARGE.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
    		Exception ex, 
    		HttpServletRequest request) {
    	
        String correlationId = getCorrelationId();
        
        log.error("Unexpected error occurred. CorrelationId: {}", correlationId, ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCode.INTERNAL_ERROR.getCode())
                .message(ErrorCode.INTERNAL_ERROR.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    private String getCorrelationId() {
    	
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
        	
            String correlationId = attributes
            		.getRequest()
            		.getHeader("X-Correlation-ID");
            
            if (correlationId != null) {
            	
                return correlationId;
            }
        }
        
        return UUID.randomUUID().toString();
    }
}