package harshal.temkar.ai.model.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizedPrompt {
	
    private String systemPrompt;
    private String userPrompt;
    private String fullPrompt;
    private Integer estimatedTokens;
    private boolean optimized;
    private String optimizationNotes;
}