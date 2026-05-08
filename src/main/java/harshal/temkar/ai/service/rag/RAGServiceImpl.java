package harshal.temkar.ai.service.rag;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import harshal.temkar.ai.config.ChatProperties;
import harshal.temkar.ai.config.RAGProperties;
import harshal.temkar.ai.exception.AiException;
import harshal.temkar.ai.exception.ErrorCode;
import harshal.temkar.ai.model.chat.AIModel;
import harshal.temkar.ai.model.chat.CitedResponse;
import harshal.temkar.ai.model.chat.DocumentChunkEntity;
import harshal.temkar.ai.model.chat.DocumentEntity;
import harshal.temkar.ai.model.chat.RAGContext;
import harshal.temkar.ai.model.conversation.ConversationMessage;
import harshal.temkar.ai.repository.DocumentChunkRepository;
import harshal.temkar.ai.repository.DocumentRepository;
import harshal.temkar.ai.service.chat.CostCalculator;
import harshal.temkar.ai.service.chat.ModelSelector;
import harshal.temkar.ai.service.chat.TokenUsageTracker;
import harshal.temkar.ai.service.constants.Constants;
import harshal.temkar.ai.service.conversation.IConversationService;
import harshal.temkar.ai.util.PromptTemplateLoader;
import harshal.temkar.ai.util.TokenCounter;
import harshal.temkar.ai.util.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RAGServiceImpl implements IRAGService {

    private final VectorStore vectorStore;
    private final ModelSelector modelSelector;
    private final TokenCounter tokenCounter;
    private final RAGProperties ragProperties;
    private final ChatProperties chatProperties;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final PromptTemplateLoader promptTemplateLoader;
    private final CostCalculator costCalculator;
    private final TokenUsageTracker usageTracker;
    private final IConversationService conversationService;

    // ==================== PUBLIC API ====================

    @Override
    public RAGContext retrieveContext(String query) {
        return retrieveContext(query, RagOptions.defaults());
    }

    @Override
    public RAGContext retrieveContext(String query, RagOptions options) {
        RagOptions opts = options == null ? RagOptions.defaults() : options;
        int topK = opts.getTopK() != null
                ? opts.getTopK()
                : ragProperties.getRetrieval().getTopK();
        double threshold = opts.getSimilarityThreshold() != null
                ? opts.getSimilarityThreshold()
                : ragProperties.getRetrieval().getSimilarityThreshold();

        log.debug("Retrieving context - query='{}', topK={}, threshold={}", query, topK, threshold);

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(threshold)
                .build();

        List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);
        log.debug("Found {} similar documents", similarDocs == null ? 0 : similarDocs.size());

        List<RAGContext.RetrievedChunk> retrievedChunks = (similarDocs == null ? List.<Document>of() : similarDocs).stream()
                .map(doc -> {
                    DocumentChunkEntity chunk = chunkRepository.findByVectorStoreId(doc.getId()).orElse(null);
                    if (chunk == null) {
                        return null;
                    }
                    DocumentEntity document = documentRepository.findById(chunk.getDocumentId()).orElse(null);
                    return RAGContext.RetrievedChunk.builder()
                            .id(doc.getId())
                            .content(doc.getFormattedContent())
                            .similarity(extractSimilarity(doc))
                            .documentId(chunk.getDocumentId())
                            .filename(document != null ? document.getFilename() : chatProperties.getUnknownFilenameLabel())
                            .chunkIndex(chunk.getChunkIndex())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        return RAGContext.builder()
                .query(query)
                .retrievedChunks(retrievedChunks)
                .combinedContext(buildCombinedContext(retrievedChunks))
                .build();
    }

    @Override
    public CitedResponse askWithRAG(String query, String sessionId) {
        return askWithRAG(query, sessionId, RagOptions.defaults());
    }

    @Override
    public CitedResponse askWithRAG(String query, String sessionId, RagOptions options) {
        RagOptions opts = options == null ? RagOptions.defaults() : options;
        log.info("RAG ask - sessionId={}, providerModel={}, topK={}, threshold={}",
                sessionId, opts.getProviderModel(), opts.getTopK(), opts.getSimilarityThreshold());

        RAGContext context = retrieveContext(query, opts);

        if (context.getRetrievedChunks().isEmpty()) {
            log.warn("No relevant context found for query: {}", query);
            return invokeNoContext(query, sessionId, opts);
        }

        String prompt = buildRAGPromptFromTemplate(query, context);
        Invocation invocation = invokeLlmWithFallback(prompt, opts);

        List<CitedResponse.Source> sources = buildSources(context.getRetrievedChunks());

        // Track usage and persist conversation (preserve the original query, not the full prompt)
        recordUsageAndConversation(query, invocation, sessionId);

        log.info("RAG query completed - provider={}, model={}, sources={}, cost=${}",
                invocation.providerKey, invocation.modelKey, sources.size(),
                invocation.usage.getEstimatedCostUsd());

        return CitedResponse.builder()
                .response(invocation.response)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .usage(invocation.usage)
                .sources(sources)
                .build();
    }

    // ==================== INTERNALS ====================

    private CitedResponse invokeNoContext(String query, String sessionId, RagOptions opts) {
        Map<String, String> variables = new HashMap<>();
        variables.put(Constants.VAR_QUERY, query);

        String templateName = ragProperties.getPrompts().getNoContextTemplate();
        String prompt = promptTemplateLoader.loadTemplate(templateName, variables);

        Invocation invocation = invokeLlmWithFallback(prompt, opts);
        recordUsageAndConversation(query, invocation, sessionId);

        return CitedResponse.builder()
                .response(invocation.response)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .usage(invocation.usage)
                .sources(List.of())
                .build();
    }

    /**
     * Call the LLM with provider fallback semantics consistent with ChatServiceImpl.
     * Applies temperature / maxTokens / model overrides where available.
     */
    private Invocation invokeLlmWithFallback(String prompt, RagOptions opts) {
        String preferredProvider = opts.getProviderModel() != null
                ? opts.getProviderModel().getProviderName()
                : null;
        List<String> chain = modelSelector.buildFallbackChain(preferredProvider);

        Exception lastError = null;
        for (String provider : chain) {
            try {
                ChatClient client = modelSelector.selectClient(provider);
                ChatClient.ChatClientRequestSpec spec = client.prompt().user(prompt);

                ChatOptions.Builder optsBuilder = ChatOptions.builder();
                AIModel pm = opts.getProviderModel();
                if (pm != null && pm.getProviderName().equalsIgnoreCase(provider)) {
                    optsBuilder.model(pm.getModelName());
                }
                if (opts.getTemperature() != null) {
                    optsBuilder.temperature(opts.getTemperature());
                }
                if (opts.getMaxTokens() != null) {
                    optsBuilder.maxTokens(opts.getMaxTokens());
                }
                spec.options(optsBuilder.build());

                String response = spec.call().content();

                String modelKey = resolveModelKey(opts.getProviderModel(), provider);
                TokenUsage usage = tokenCounter.calculateUsage(prompt, response);
                usage.setModel(modelKey);
                usage.setEstimatedCostUsd(costCalculator.calculate(
                        modelKey, usage.getPromptTokens(), usage.getCompletionTokens()));
                usage.setCached(Boolean.FALSE);

                return new Invocation(response, usage, provider, modelKey);

            } catch (Exception ex) {
                lastError = ex;
                log.warn("RAG provider '{}' failed: {} – attempting next in fallback chain",
                        provider, ex.getMessage());
            }
        }

        log.error("All providers in fallback chain failed for RAG: {}", chain, lastError);
        throw new AiException(ErrorCode.AI_SERVICE_ERROR,
                "All LLM providers failed during RAG. Last error: "
                        + (lastError != null ? lastError.getMessage() : "unknown"),
                lastError);
    }

    private void recordUsageAndConversation(String userQuery, Invocation inv, String sessionId) {
        usageTracker.record(sessionId, inv.usage);
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        try {
            conversationService.saveMessage(sessionId, ConversationMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .role(Constants.ROLE_USER)
                    .content(userQuery)
                    .timestamp(LocalDateTime.now())
                    .tokenCount(inv.usage.getPromptTokens())
                    .build());
            conversationService.saveMessage(sessionId, ConversationMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .role(Constants.ROLE_ASSISTANT)
                    .content(inv.response)
                    .timestamp(LocalDateTime.now())
                    .tokenCount(inv.usage.getCompletionTokens())
                    .build());
        } catch (Exception ex) {
            log.error("Failed to persist RAG conversation for session {}", sessionId, ex);
        }
    }

    /** Resolve a "provider:model" key for pricing/tracking. */
    private String resolveModelKey(AIModel requested, String activeProvider) {
        if (requested != null && requested.getProviderName().equalsIgnoreCase(activeProvider)) {
            return (requested.getProviderName() + Constants.DEFAULT_PROVIDER_KEY_SEPARATOR + requested.getModelName()).toLowerCase();
        }
        AIModel def = modelSelector.getDefaultModel();
        if (def != null && def.getProviderName().equalsIgnoreCase(activeProvider)) {
            return (def.getProviderName() + Constants.DEFAULT_PROVIDER_KEY_SEPARATOR + def.getModelName()).toLowerCase();
        }
        return activeProvider == null ? Constants.PROVIDER_UNKNOWN : activeProvider.toLowerCase();
    }

    private String buildRAGPromptFromTemplate(String query, RAGContext context) {
        Map<String, String> variables = new HashMap<>();
        variables.put(Constants.VAR_CONTEXT, context.getCombinedContext());
        variables.put(Constants.VAR_QUERY, query);
        String templateName = ragProperties.getPrompts().getRagTemplate();
        return promptTemplateLoader.loadTemplate(templateName, variables);
    }

    private String buildCombinedContext(List<RAGContext.RetrievedChunk> chunks) {
        String templatePath = ragProperties.getPrompts().getSourceBlockTemplate();
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RAGContext.RetrievedChunk chunk = chunks.get(i);
            Map<String, String> vars = new HashMap<>();
            vars.put(Constants.VAR_SOURCE_ID, String.valueOf(i + 1));
            vars.put(Constants.VAR_FILENAME, chunk.getFilename());
            vars.put(Constants.VAR_CONTENT, chunk.getContent());
            context.append(promptTemplateLoader.loadTemplate(templatePath, vars));
        }
        return context.toString();
    }

    private List<CitedResponse.Source> buildSources(List<RAGContext.RetrievedChunk> chunks) {
        List<CitedResponse.Source> sources = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            RAGContext.RetrievedChunk chunk = chunks.get(i);
            sources.add(CitedResponse.Source.builder()
                    .id(String.valueOf(i + 1))
                    .filename(chunk.getFilename())
                    .chunkIndex(chunk.getChunkIndex())
                    .relevanceScore(chunk.getSimilarity())
                    .excerpt(truncateExcerpt(chunk.getContent()))
                    .build());
        }
        return sources;
    }

    private String truncateExcerpt(String content) {
        if (content == null) return Constants.EMPTY_STRING;
        int max = chatProperties.getExcerptMaxLength();
        String suffix = chatProperties.getExcerptTruncationSuffix();
        if (content.length() <= max) return content;
        int cut = Math.max(0, max - suffix.length());
        return content.substring(0, cut) + suffix;
    }

    /**
     * Best-effort extraction of a similarity score (0-1) from a retrieved Document.
     * Spring AI's pgvector store populates {@code Document.getScore()} with the
     * cosine similarity. Falls back to 1 - distance if 'distance' metadata is set,
     * else 0.0 when nothing is available.
     */
    private double extractSimilarity(Document doc) {
        try {
            Double score = doc.getScore();
            if (score != null) {
                return score;
            }
        } catch (Throwable ignore) {
            // older Spring AI versions may not expose getScore(); fall through
        }
        Object distance = doc.getMetadata() != null ? doc.getMetadata().get(Constants.META_DISTANCE) : null;
        if (distance instanceof Number n) {
            return Math.max(0.0d, 1.0d - n.doubleValue());
        }
        return 0.0d;
    }

    /** Local record-like holder for a single LLM invocation result. */
    private static final class Invocation {
        final String response;
        final TokenUsage usage;
        final String providerKey;
        final String modelKey;

        Invocation(String response, TokenUsage usage, String providerKey, String modelKey) {
            this.response = response;
            this.usage = usage;
            this.providerKey = providerKey;
            this.modelKey = modelKey;
        }
    }
}