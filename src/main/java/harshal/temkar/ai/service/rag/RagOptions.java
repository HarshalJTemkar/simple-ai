package harshal.temkar.ai.service.rag;

import harshal.temkar.ai.model.chat.AIModel;
import lombok.Builder;
import lombok.Value;

/**
 * Per-request overrides for a RAG call. All fields are optional; null values mean
 * "fall back to configuration / sensible defaults".
 */
@Value
@Builder
public class RagOptions {

    AIModel providerModel;       // override default LLM provider+model for the answer call
    Double temperature;          // override default temperature
    Integer maxTokens;           // override default max output tokens
    Integer topK;                // override RAG retrieval top-K
    Double similarityThreshold;  // override RAG similarity threshold (0..1)

    public static RagOptions defaults() {
        return RagOptions.builder().build();
    }
}
