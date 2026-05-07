package harshal.temkar.ai.service.rag;

import harshal.temkar.ai.model.chat.CitedResponse;
import harshal.temkar.ai.model.chat.RAGContext;

public interface IRAGService {
    
    RAGContext retrieveContext(String query);
    
    RAGContext retrieveContext(String query, RagOptions options);
    
    CitedResponse askWithRAG(String query, String sessionId);
    
    CitedResponse askWithRAG(String query, String sessionId, RagOptions options);
}