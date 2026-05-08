package harshal.temkar.ai.service.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import harshal.temkar.ai.exception.AiException;
import harshal.temkar.ai.exception.ErrorCode;
import harshal.temkar.ai.config.ChatProperties;
import harshal.temkar.ai.model.chat.AIModel;
import harshal.temkar.ai.model.chat.ChatRequest;
import harshal.temkar.ai.model.chat.ChatResponse;
import harshal.temkar.ai.model.chat.StreamingChatResponse;
import harshal.temkar.ai.model.conversation.ConversationMessage;
import harshal.temkar.ai.service.constants.Constants;
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
    private final CostCalculator costCalculator;
    private final TokenUsageTracker usageTracker;
    private final ChatProperties chatProperties;

    @Override
    public ChatResponse ask(ChatRequest request) {
        // Cache only deterministic, stateless calls (no session, low/zero temperature)
        if (isCacheable(request)) {
            return cachedAsk(cacheKey(request), request);
        }
        return doAsk(request);
    }

    /**
     * Cache layer: keyed by provider:model + message + temperature/maxTokens.
     * Identical prompts within TTL return without hitting the LLM (0 new tokens).
     */
    @Cacheable(cacheNames = Constants.CACHE_CHAT_RESPONSES,
               cacheManager = Constants.CACHE_MANAGER_RESPONSE, key = "#key")
    public ChatResponse cachedAsk(String key, ChatRequest request) {
        log.debug("Cache MISS for key={}", key);
        ChatResponse resp = doAsk(request);
        if (resp != null && resp.getUsage() != null) {
            // Mark so subsequent reads can be identified as cached on the tracker side too
            resp.getUsage().setCached(Boolean.FALSE);
        }
        return resp;
    }

    private ChatResponse doAsk(ChatRequest request) {
        List<String> chain = modelSelector.buildFallbackChain(getProviderName(request));
        Exception lastError = null;

        for (String provider : chain) {
            try {
                log.debug("Attempting chat with provider '{}' (chain={})", provider, chain);
                ChatClient chatClient = modelSelector.selectClient(provider);

                String promptMessage = buildPromptWithContext(request);
                ChatClient.ChatClientRequestSpec spec = chatClient.prompt().user(promptMessage);
                spec = applyOptions(spec, request, provider);

                String response = spec.call().content();

                String modelKey = resolveModelKey(request, provider);
                TokenUsage usage = tokenCounter.calculateUsage(request.getMessage(), response);
                usage.setModel(modelKey);
                usage.setEstimatedCostUsd(costCalculator.calculate(
                        modelKey, usage.getPromptTokens(), usage.getCompletionTokens()));
                usage.setCached(Boolean.FALSE);

                usageTracker.record(request.getSessionId(), usage);
                saveConversation(request.getSessionId(), request.getMessage(), response, usage);

                log.info("Chat OK - provider={}, model={}, tokens={}, cost=${}",
                        provider, modelKey, usage.getTotalTokens(), usage.getEstimatedCostUsd());

                return new ChatResponse(response, request.getSessionId(), usage);

            } catch (Exception ex) {
                lastError = ex;
                log.warn("Provider '{}' failed: {} – attempting next in fallback chain", provider, ex.getMessage());
            }
        }

        log.error("All providers in fallback chain failed: {}", chain, lastError);
        throw new AiException(ErrorCode.AI_SERVICE_ERROR,
                "All LLM providers failed. Last error: "
                        + (lastError != null ? lastError.getMessage() : "unknown"),
                lastError);
    }

    @Override
    public Flux<StreamingChatResponse> askStreaming(ChatRequest request) {
        try {
            log.debug("Processing streaming request - Provider: {}, Session: {}",
                      getProviderName(request), request.getSessionId());

            String messageId = UUID.randomUUID().toString();
            AtomicInteger tokenCount = new AtomicInteger(0);
            StringBuilder fullResponse = new StringBuilder();

            ChatClient chatClient = selectChatClient(request);
            String resolvedProvider = getProviderName(request);
            String modelKey = resolveModelKey(request, resolvedProvider);

            String promptMessage = buildPromptWithContext(request);
            ChatClient.ChatClientRequestSpec spec = chatClient.prompt().user(promptMessage);
            spec = applyOptions(spec, request, resolvedProvider);

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
                            buildFinalResponse(messageId, request, fullResponse.toString(), modelKey)
                    ))
                    .doOnComplete(() -> {
                        TokenUsage usage = tokenCounter.calculateUsage(
                            request.getMessage(),
                            fullResponse.toString()
                        );
                        usage.setModel(modelKey);
                        usage.setEstimatedCostUsd(costCalculator.calculate(
                                modelKey, usage.getPromptTokens(), usage.getCompletionTokens()));
                        usage.setCached(Boolean.FALSE);
                        usageTracker.record(request.getSessionId(), usage);
                        saveConversation(request.getSessionId(), request.getMessage(),
                                       fullResponse.toString(), usage);
                        log.debug("Streaming completed - Tokens: {}, Cost: ${}",
                                tokenCount.get(), usage.getEstimatedCostUsd());
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

    private boolean isCacheable(ChatRequest request) {
        // Don't cache session-aware (history-dependent) calls or non-deterministic ones
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) return false;
        Double t = request.getTemperature();
        return t == null || t <= 0.0d;
    }

    private String cacheKey(ChatRequest request) {
        String pm = request.getProviderModel() == null ? "default"
                : request.getProviderModel().getProviderName() + Constants.DEFAULT_PROVIDER_KEY_SEPARATOR
                  + request.getProviderModel().getModelName();
        return pm + "|t=" + request.getTemperature() + "|m=" + request.getMaxTokens()
                + "|q=" + (request.getMessage() == null ? Constants.EMPTY_STRING : request.getMessage().trim());
    }

    private ChatClient selectChatClient(ChatRequest request) {
        if (request.getProviderModel() != null) {
            return modelSelector.selectClient(request.getProviderModel().getProviderName());
        }
        return modelSelector.getDefaultClient();
    }

    private String buildPromptWithContext(ChatRequest request) {
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            return conversationService.buildPromptWithContext(
                request.getSessionId(),
                request.getMessage(),
                chatProperties.getHistoryContextLimit()
            );
        }
        return request.getMessage();
    }

    private ChatClient.ChatClientRequestSpec applyOptions(
            ChatClient.ChatClientRequestSpec spec, ChatRequest request, String activeProvider) {

        ChatOptions.Builder optionsBuilder = ChatOptions.builder();

        if (request.getProviderModel() != null
                && request.getProviderModel().getProviderName().equalsIgnoreCase(activeProvider)) {
            // Only pass the requested model name when we are actually on the requested provider.
            optionsBuilder.model(request.getProviderModel().getModelName());
        }
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
                    .role(Constants.ROLE_USER)
                    .content(userMessage)
                    .timestamp(LocalDateTime.now())
                    .tokenCount(usage.getPromptTokens())
                    .build());

            conversationService.saveMessage(sessionId, ConversationMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .role(Constants.ROLE_ASSISTANT)
                    .content(assistantMessage)
                    .timestamp(LocalDateTime.now())
                    .tokenCount(usage.getCompletionTokens())
                    .build());
        } catch (Exception ex) {
            log.error("Failed to save conversation", ex);
        }
    }

    private StreamingChatResponse buildFinalResponse(String messageId, ChatRequest request,
                                                     String fullResponse, String modelKey) {
        TokenUsage usage = tokenCounter.calculateUsage(request.getMessage(), fullResponse);
        usage.setModel(modelKey);
        usage.setEstimatedCostUsd(costCalculator.calculate(
                modelKey, usage.getPromptTokens(), usage.getCompletionTokens()));

        return StreamingChatResponse.builder()
                .id(messageId)
                .content("")
                .sessionId(request.getSessionId())
                .done(true)
                .timestamp(LocalDateTime.now())
                .usage(usage)
                .build();
    }

    private String getProviderName(ChatRequest request) {
        return request.getProviderModel() != null
            ? request.getProviderModel().getProviderName()
            : null;
    }

    /** Resolve a "provider:model" key for pricing/tracking. */
    private String resolveModelKey(ChatRequest request, String activeProvider) {
        AIModel pm = request.getProviderModel();
        if (pm != null && pm.getProviderName().equalsIgnoreCase(activeProvider)) {
            return (pm.getProviderName() + Constants.DEFAULT_PROVIDER_KEY_SEPARATOR + pm.getModelName()).toLowerCase();
        }
        AIModel def = modelSelector.getDefaultModel();
        if (def != null && def.getProviderName().equalsIgnoreCase(activeProvider)) {
            return (def.getProviderName() + Constants.DEFAULT_PROVIDER_KEY_SEPARATOR + def.getModelName()).toLowerCase();
        }
        return activeProvider == null ? Constants.PROVIDER_UNKNOWN : activeProvider.toLowerCase();
    }
}
