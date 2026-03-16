package harshal.temkar.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.rag")
public class RAGProperties {
    
    private Chunking chunking = new Chunking();
    private Retrieval retrieval = new Retrieval();
    private Prompts prompts = new Prompts();
    private boolean enabled = true;
    
    @Data
    public static class Chunking {
        private Integer chunkSize; // Characters per chunk
        private Integer chunkOverlap; // Overlap between chunks
        private String strategy; // sentence, paragraph, fixed
    }
    
    @Data
    public static class Retrieval {
        private Integer topK; // Number of similar chunks to retrieve
        private Double similarityThreshold; // Minimum similarity score (0-1)
        private String searchType; // similarity, mmr
    }
    
    @Data
    public static class Prompts {
        private String ragTemplate;
        private String noContextTemplate;
        private String citationGuidelines;
    }
}