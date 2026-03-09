package harshal.temkar.ai.service.chat;


import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import harshal.temkar.ai.exception.AiException;
import harshal.temkar.ai.exception.ErrorCode;
import harshal.temkar.ai.model.chat.ChatRequest;
import harshal.temkar.ai.model.chat.ChatResponse;
import harshal.temkar.ai.model.chat.StreamingChatResponse;
import harshal.temkar.ai.util.TokenCounter;
import harshal.temkar.ai.util.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements IChatService {

	private final ChatClient chatClient;
	private final TokenCounter tokenCounter;

	@Override
	public ChatResponse ask(
			ChatRequest request) {
		
		try {
			log.debug("Processing chat request for session: {}", request.getSessionId());

			String response = chatClient
					.prompt()
					.user(request.getMessage())
					.call()
					.content();

			log.debug("AI response generated successfully for session: {}", request.getSessionId());

			TokenUsage usage = tokenCounter.calculateUsage(request.getMessage(), response);
			
			return new ChatResponse(response, request.getSessionId(), usage);


		} catch (Exception ex) {
			
			log.error("Failed to process chat request for session: {}", request.getSessionId(), ex);
			throw new AiException(ErrorCode.AI_SERVICE_ERROR, "Failed to communicate with AI service", ex);
		}
	}

	@Override
    public Flux<StreamingChatResponse> askStreaming(ChatRequest request) {
        try {
            log.debug("Processing streaming chat request for session: {}", request.getSessionId());
            
            String messageId = UUID.randomUUID().toString();
            AtomicInteger tokenCount = new AtomicInteger(0);
            StringBuilder fullResponse = new StringBuilder();
            
            return chatClient
					.prompt()
					.user(request.getMessage())
					.stream()
					.content()
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
                    		createFinalStreamingResponse(
                                    messageId,
                                    request,
                                    fullResponse.toString(),
                                    tokenCount.get()
                            )
                    ))
                    .doOnComplete(() -> 
                        log.debug("Streaming completed for session: {}. Total tokens: {}, text length: {}", 
                                  request.getSessionId(), tokenCount.get(), fullResponse.length())
                    )
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
}