package harshal.temkar.ai.model.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGContext {
    
    private String query;
    private List<RetrievedChunk> retrievedChunks;
    private String combinedContext;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrievedChunk {
        private String id;
        private String content;
        private Double similarity;
        private Long documentId;
        private String filename;
        private Integer chunkIndex;
    }
}