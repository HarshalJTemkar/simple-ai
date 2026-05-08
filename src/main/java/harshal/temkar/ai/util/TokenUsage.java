package harshal.temkar.ai.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsage {

    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;

    /** Provider:model used for this call (e.g., "ollama:llama3"). */
    private String model;

    /** Estimated cost in USD for this call (0 for local Ollama). */
    private Double estimatedCostUsd;

    /** True when the response was served from cache (no LLM call). */
    private Boolean cached;

    public TokenUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }
}