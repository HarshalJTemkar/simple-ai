package harshal.temkar.ai.service.chat;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import harshal.temkar.ai.exception.AiException;
import harshal.temkar.ai.exception.ErrorCode;
import harshal.temkar.ai.model.chat.ChatRequest;
import harshal.temkar.ai.model.chat.ChatResponse;
import harshal.temkar.ai.model.chat.StreamingChatResponse;
import harshal.temkar.ai.model.chat.AIModel;
import harshal.temkar.ai.model.conversation.ConversationMessage;
import harshal.temkar.ai.service.conversation.IConversationService;
import harshal.temkar.ai.util.TokenCounter;
import harshal.temkar.ai.util.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements IChatService {

    private final ModelSelector modelSelector;
    private final TokenCounter tokenCounter;
    private final IConversationService conversationService;

    @Override
    public ChatResponse ask(ChatRequest request) {
        try {
            // Get model from request or use default
            AIModel model = resolveModel(request);
            
            log.debug("Processing chat request - Model: {} ({}), Session: {}", 
                      model, model.getModelName(), request.getSessionId());
            
            // Select ChatClient based on model
            ChatClient chatClient = modelSelector.selectClient(model);
            
            // Build prompt with context
            String promptMessage = buildPromptWithContext(request);
            
            // Build and execute request
            ChatClient.ChatClientRequestSpec spec = chatClient
                    .prompt()
                    .user(promptMessage);
            
            spec = applyOptions(spec, request, model);
            
            String response = spec.call().content();
            
            // Calculate tokens
            TokenUsage usage = tokenCounter.calculateUsage(request.getMessage(), response);
            
            // Save conversation
            saveConversation(request.getSessionId(), request.getMessage(), response, usage);
            
            log.debug("Chat completed - Model: {}, Tokens: {}", 
                      model.getModelName(), usage.getTotalTokens());
            
            return new ChatResponse(response, request.getSessionId(), usage);
            
        } catch (Exception ex) {
            log.error("Chat failed - Session: {}", request.getSessionId(), ex);
            throw new AiException(ErrorCode.AI_SERVICE_ERROR, 
                                  "Failed to communicate with AI service: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Flux<StreamingChatResponse> askStreaming(ChatRequest request) {
        try {
            AIModel model = resolveModel(request);
            
            log.debug("Processing streaming request - Model: {}, Session: {}", 
                      model.getModelName(), request.getSessionId());
            
            String messageId = UUID.randomUUID().toString();
            AtomicInteger tokenCount = new AtomicInteger(0);
            StringBuilder fullResponse = new StringBuilder();
            
            // Select ChatClient
            ChatClient chatClient = modelSelector.selectClient(model);
            
            // Build prompt
            String promptMessage = buildPromptWithContext(request);
            
            ChatClient.ChatClientRequestSpec spec = chatClient
                    .prompt()
                    .user(promptMessage);
            
            spec = applyOptions(spec, request, model);
            
            Flux<String> contentFlux = spec.stream().content();
            
            return contentFlux
                    .map(token -> {
                        fullResponse.append(token);
                        int count = tokenCounter.estimateTokenCount(token);
                        tokenCount.addAndGet(count);
                        
                        return StreamingChatResponse.builder()
                                .id(messageId)
                                .content(token)
                                .sessionId(request.getSessionId())
                                .done(false)
                                .timestamp(LocalDateTime.now())
                                .currentTokens(count)
                                .build();
                    })
                    .concatWith(Flux.just(
                            buildFinalResponse(messageId, request, fullResponse.toString(), tokenCount.get())
                    ))
                    .doOnComplete(() -> {
                        TokenUsage usage = tokenCounter.calculateUsage(
                            request.getMessage(), 
                            fullResponse.toString()
                        );
                        saveConversation(request.getSessionId(), request.getMessage(), 
                                       fullResponse.toString(), usage);
                        log.debug("Streaming completed - Tokens: {}", tokenCount.get());
                    })
                    .doOnError(error -> 
                        log.error("Streaming failed - Session: {}", request.getSessionId(), error)
                    );
                    
        } catch (Exception ex) {
            log.error("Streaming initialization failed", ex);
            return Flux.just(
                    StreamingChatResponse.builder()
                            .error("Failed to start streaming: " + ex.getMessage())
                            .errorCode(ErrorCode.AI_SERVICE_ERROR.getCode())
                            .sessionId(request.getSessionId())
                            .done(true)
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Resolve AIModel from request or use default
     */
    private AIModel resolveModel(ChatRequest request) {
        if (request.getProviderModel() != null) {
            return request.getProviderModel();
        }
        
        // Use default model from configuration
        AIModel defaultModel = modelSelector.getDefaultModel();
        log.debug("No model specified in request, using default: {}", defaultModel);
        return defaultModel;
    }
    
    private String buildPromptWithContext(ChatRequest request) {
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            return conversationService.buildPromptWithContext(
                request.getSessionId(), 
                request.getMessage(), 
                5
            );
        }
        return request.getMessage();
    }
    
    private ChatClient.ChatClientRequestSpec applyOptions(
            ChatClient.ChatClientRequestSpec spec, ChatRequest request, AIModel model) {
        
        ChatOptions.Builder optionsBuilder = ChatOptions.builder();
        
        // Always set the model name
        optionsBuilder.model(model.getModelName());
        log.debug("Using model: {}", model.getModelName());
        
        if (request.getTemperature() != null) {
            optionsBuilder.temperature(request.getTemperature());
        }
        
        if (request.getMaxTokens() != null) {
            optionsBuilder.maxTokens(request.getMaxTokens());
        }
        
        spec.options(optionsBuilder.build());
        return spec;
    }
    
    private void saveConversation(String sessionId, String userMessage, 
                                  String assistantMessage, TokenUsage usage) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        
        try {
            conversationService.saveMessage(sessionId, ConversationMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .role("user")
                    .content(userMessage)
                    .timestamp(LocalDateTime.now())
                    .tokenCount(usage.getPromptTokens())
                    .build());
            
            conversationService.saveMessage(sessionId, ConversationMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .role("assistant")
                    .content(assistantMessage)
                    .timestamp(LocalDateTime.now())
                    .tokenCount(usage.getCompletionTokens())
                    .build());
        } catch (Exception ex) {
            log.error("Failed to save conversation", ex);
        }
    }
    
    private StreamingChatResponse buildFinalResponse(String messageId, ChatRequest request, 
                                                     String fullResponse, int totalTokens) {
        TokenUsage usage = tokenCounter.calculateUsage(request.getMessage(), fullResponse);
        
        return StreamingChatResponse.builder()
                .id(messageId)
                .content("")
                .sessionId(request.getSessionId())
                .done(true)
                .timestamp(LocalDateTime.now())
                .usage(usage)
                .build();
    }
}