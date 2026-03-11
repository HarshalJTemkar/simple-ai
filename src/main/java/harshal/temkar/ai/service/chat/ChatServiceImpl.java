package harshal.temkar.ai.service.chat;

import harshal.temkar.ai.conversation.model.ConversationMessage;
import harshal.temkar.ai.conversation.service.IConversationService;
import harshal.temkar.ai.exception.AiException;
import harshal.temkar.ai.exception.ErrorCode;
import harshal.temkar.ai.model.chat.ChatRequest;
import harshal.temkar.ai.model.chat.ChatResponse;
import harshal.temkar.ai.model.chat.StreamingChatResponse;
import harshal.temkar.ai.util.TokenCounter;
import harshal.temkar.ai.util.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements IChatService {

    private final ChatClient chatClient;
    private final TokenCounter tokenCounter;
    private final IConversationService conversationService;

    @Override
    public ChatResponse ask(ChatRequest request) {
        try {
            log.debug("Processing chat request for session: {}", request.getSessionId());
            
            // Build prompt with conversation context if sessionId provided
            String promptMessage = request.getMessage();
            if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
                promptMessage = conversationService.buildPromptWithContext(
                    request.getSessionId(), 
                    request.getMessage(), 
                    5 // Last 5 messages for context
                );
                log.debug("Added conversation context for session: {}", request.getSessionId());
            }
            
            ChatClient.ChatClientRequestSpec spec = chatClient
                    .prompt()
                    .user(promptMessage);
            
            String response = spec.call().content();
            
            // Calculate token usage
            TokenUsage usage = tokenCounter.calculateUsage(request.getMessage(), response);
            
            // Save conversation messages
            if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
                saveConversationMessages(request.getSessionId(), request.getMessage(), response, usage);
            }
            
            log.debug("AI response generated. Session: {}, Tokens: {}", 
                      request.getSessionId(), usage.getTotalTokens());
            
            ChatResponse chatResponse = new ChatResponse(response, request.getSessionId(), usage);
            
            return chatResponse;
            
        } catch (Exception ex) {
            log.error("Failed to process chat request for session: {}", request.getSessionId(), ex);
            throw new AiException(ErrorCode.AI_SERVICE_ERROR, 
                                  "Failed to communicate with AI service", ex);
        }
    }

    @Override
    public Flux<StreamingChatResponse> askStreaming(ChatRequest request) {
        try {
            log.debug("Processing streaming chat request for session: {}", request.getSessionId());
            
            String messageId = UUID.randomUUID().toString();
            AtomicInteger currentTokenCount = new AtomicInteger(0);
            StringBuilder fullResponse = new StringBuilder();
            
            // Build prompt with context
            String promptMessage = request.getMessage();
            if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
                promptMessage = conversationService.buildPromptWithContext(
                    request.getSessionId(), 
                    request.getMessage(), 
                    5
                );
            }
            
            ChatClient.ChatClientRequestSpec spec = chatClient
                    .prompt()
                    .user(promptMessage);

            Flux<String> contentFlux = spec.stream().content();
            
            return contentFlux
                    .map(token -> {
                        fullResponse.append(token);
                        int tokenCount = tokenCounter.estimateTokenCount(token);
                        currentTokenCount.addAndGet(tokenCount);
                        
                        return StreamingChatResponse.builder()
                                .id(messageId)
                                .content(token)
                                .sessionId(request.getSessionId())
                                .done(false)
                                .timestamp(LocalDateTime.now())
                                .currentTokens(tokenCount)
                                .build();
                    })
                    .concatWith(Flux.just(
                            createFinalStreamingResponse(
                                    messageId,
                                    request,
                                    fullResponse.toString(),
                                    currentTokenCount.get()
                            )
                    ))
                    .doOnComplete(() -> {
                        // Save conversation after streaming completes
                        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
                            TokenUsage usage = tokenCounter.calculateUsage(
                                request.getMessage(), 
                                fullResponse.toString()
                            );
                            saveConversationMessages(
                                request.getSessionId(), 
                                request.getMessage(), 
                                fullResponse.toString(), 
                                usage
                            );
                        }
                        log.debug("Streaming completed. Session: {}, Total tokens: {}", 
                                  request.getSessionId(), currentTokenCount.get());
                    })
                    .doOnError(error -> 
                        log.error("Streaming failed for session: {}", request.getSessionId(), error)
                    );
                    
        } catch (Exception ex) {
            log.error("Failed to initialize streaming for session: {}", request.getSessionId(), ex);
            
            return Flux.just(
                    StreamingChatResponse.builder()
                            .error("Failed to start streaming")
                            .errorCode(ErrorCode.AI_SERVICE_ERROR.getCode())
                            .sessionId(request.getSessionId())
                            .done(true)
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        }
    }
    
    private StreamingChatResponse createFinalStreamingResponse(
            String messageId,
            ChatRequest request,
            String fullResponse,
            int completionTokens) {
        
        TokenUsage usage = tokenCounter.calculateUsage(
                request.getMessage(), 
                fullResponse
        );
        
        return StreamingChatResponse.builder()
                .id(messageId)
                .content("")
                .sessionId(request.getSessionId())
                .done(true)
                .timestamp(LocalDateTime.now())
                .usage(usage)
                .build();
    }
    
    private void saveConversationMessages(String sessionId, String userMessage, 
                                         String assistantMessage, TokenUsage usage) {
        try {
            // Save user message
            conversationService.saveMessage(sessionId, ConversationMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .role("user")
                    .content(userMessage)
                    .timestamp(LocalDateTime.now())
                    .tokenCount(usage.getPromptTokens())
                    .build());
            
            // Save assistant message
            conversationService.saveMessage(sessionId, ConversationMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .role("assistant")
                    .content(assistantMessage)
                    .timestamp(LocalDateTime.now())
                    .tokenCount(usage.getCompletionTokens())
                    .build());
                    
            log.debug("Saved conversation messages for session: {}", sessionId);
        } catch (Exception ex) {
            log.error("Failed to save conversation messages", ex);
        }
    }
}