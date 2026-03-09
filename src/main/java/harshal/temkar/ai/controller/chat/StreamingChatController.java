package harshal.temkar.ai.controller.chat;

import harshal.temkar.ai.model.chat.ChatRequest;
import harshal.temkar.ai.model.chat.StreamingChatResponse;
import harshal.temkar.ai.service.chat.IChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Streaming Chat API", description = "Real-time AI Chat with Server-Sent Events")
public class StreamingChatController {

    private final IChatService chatService;

    // Keep POST for programmatic access
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream chat response (POST)")
    public Flux<StreamingChatResponse> streamChatPost(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        log.info("Received POST streaming request. CorrelationId: {}, SessionId: {}", 
                 correlationId, request.getSessionId());
        
        return chatService.askStreaming(request)
                .delayElements(Duration.ofMillis(10));
    }

    // ADD: GET for EventSource (Browser SSE)
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream chat response (GET)", 
               description = "For EventSource/SSE - pass message as query parameter")
    public Flux<StreamingChatResponse> streamChatGet(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        log.info("Received GET streaming request. CorrelationId: {}, Message: {}", 
                 correlationId, message);
        
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        request.setSessionId(sessionId);
        
        return chatService.askStreaming(request)
                .delayElements(Duration.ofMillis(10));
    }
}