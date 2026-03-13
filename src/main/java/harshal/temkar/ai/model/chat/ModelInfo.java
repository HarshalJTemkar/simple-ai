package harshal.temkar.ai.model.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {
	
    private String provider; // Provider from AIModel enum
    private String model; // Model name from AIModel enum
    private boolean enabled; // From application.yaml properties
    private boolean accessible; // Test if model is actually accessible
    private Double defaultTemperature; // From properties
    private Long maxTokens; // From properties
}