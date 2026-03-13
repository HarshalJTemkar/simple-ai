package harshal.temkar.ai.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenUsage {
	
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
}