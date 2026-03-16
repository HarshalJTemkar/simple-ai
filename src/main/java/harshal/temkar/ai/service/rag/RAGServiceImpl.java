package harshal.temkar.ai.service.rag;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import harshal.temkar.ai.config.RAGProperties;
import harshal.temkar.ai.model.chat.CitedResponse;
import harshal.temkar.ai.model.chat.DocumentChunkEntity;
import harshal.temkar.ai.model.chat.DocumentEntity;
import harshal.temkar.ai.model.chat.RAGContext;
import harshal.temkar.ai.repository.DocumentChunkRepository;
import harshal.temkar.ai.repository.DocumentRepository;
import harshal.temkar.ai.service.chat.ModelSelector;
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
    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final PromptTemplateLoader promptTemplateLoader;

    @Override
    public RAGContext retrieveContext(String query) {
        log.debug("Retrieving context for query: {}", query);
        
        SearchRequest searchRequest = SearchRequest.builder()
        		.query(query)
                .topK(ragProperties.getRetrieval().getTopK())
                .similarityThreshold(ragProperties.getRetrieval().getSimilarityThreshold())
                .build();
        
        List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);
        
        log.debug("Found {} similar documents", similarDocs.size());
        
        List<RAGContext.RetrievedChunk> retrievedChunks = similarDocs.stream()
                .map(doc -> {
                    String vectorId = doc.getId();
                    DocumentChunkEntity chunk = chunkRepository.findByVectorStoreId(vectorId)
                            .orElse(null);
                    
                    if (chunk != null) {
                        DocumentEntity document = 
                            documentRepository.findById(chunk.getDocumentId()).orElse(null);
                        
                        return RAGContext.RetrievedChunk.builder()
                                .id(doc.getId())
                                .content(doc.getFormattedContent())
                                .similarity(1.0) // pgvector doesn't return score directly
                                .documentId(chunk.getDocumentId())
                                .filename(document != null ? document.getFilename() : "Unknown")
                                .chunkIndex(chunk.getChunkIndex())
                                .build();
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        
        String combinedContext = buildCombinedContext(retrievedChunks);
        
        return RAGContext.builder()
                .query(query)
                .retrievedChunks(retrievedChunks)
                .combinedContext(combinedContext)
                .build();
    }

    @Override
    public CitedResponse askWithRAG(String query, String sessionId) {
        log.info("Processing RAG query: {}", query);
        
        RAGContext context = retrieveContext(query);
        
        if (context.getRetrievedChunks().isEmpty()) {
            log.warn("No relevant context found for query: {}", query);
            return createNoContextResponse(query, sessionId);
        }
        
        // Build prompt using template
        String prompt = buildRAGPromptFromTemplate(query, context);
        
        // Get AI response
        ChatClient chatClient = modelSelector.getDefaultClient();
        String aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        
        TokenUsage usage = tokenCounter.calculateUsage(prompt, aiResponse);
        
        List<CitedResponse.Source> sources = buildSources(context.getRetrievedChunks());
        
        log.info("RAG query completed with {} sources", sources.size());
        
        return CitedResponse.builder()
                .response(aiResponse)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .usage(usage)
                .sources(sources)
                .build();
    }

    /**
     * Build RAG prompt using external template
     */
    private String buildRAGPromptFromTemplate(String query, RAGContext context) {
        Map<String, String> variables = new HashMap<>();
        variables.put("context", context.getCombinedContext());
        variables.put("query", query);
        
        String templateName = ragProperties.getPrompts().getRagTemplate();
        return promptTemplateLoader.loadTemplate(templateName, variables);
    }

    /**
     * Build no-context response using external template
     */
    private CitedResponse createNoContextResponse(String query, String sessionId) {
        Map<String, String> variables = new HashMap<>();
        variables.put("query", query);
        
        String templateName = ragProperties.getPrompts().getNoContextTemplate();
        String prompt = promptTemplateLoader.loadTemplate(templateName, variables);
        
        ChatClient chatClient = modelSelector.getDefaultClient();
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        
        TokenUsage usage = tokenCounter.calculateUsage(prompt, response);
        
        return CitedResponse.builder()
                .response(response)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .usage(usage)
                .sources(List.of())
                .build();
    }

    private String buildCombinedContext(List<RAGContext.RetrievedChunk> chunks) {
        StringBuilder context = new StringBuilder();
        
        for (int i = 0; i < chunks.size(); i++) {
            RAGContext.RetrievedChunk chunk = chunks.get(i);
            context.append(String.format("<source id=\"%d\" name=\"%s\">%s</source>\n\n", 
                    i + 1, chunk.getFilename(), chunk.getContent()));
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
        if (content.length() <= 200) {
            return content;
        }
        return content.substring(0, 197) + "...";
    }
}