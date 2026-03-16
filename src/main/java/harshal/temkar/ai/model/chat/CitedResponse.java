package harshal.temkar.ai.model.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import harshal.temkar.ai.util.TokenUsage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CitedResponse {
    
    private String response;
    private String sessionId;
    private LocalDateTime timestamp;
    private TokenUsage usage;
    private List<Source> sources;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
    	
        private String id;
        private String filename;
        private Integer chunkIndex;
        private Double relevanceScore;
        private String excerpt;
    }
}