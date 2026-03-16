package harshal.temkar.ai.service.rag;

import harshal.temkar.ai.model.chat.CitedResponse;
import harshal.temkar.ai.model.chat.RAGContext;

public interface IRAGService {
    
    RAGContext retrieveContext(String query);
    
    CitedResponse askWithRAG(String query, String sessionId);
}