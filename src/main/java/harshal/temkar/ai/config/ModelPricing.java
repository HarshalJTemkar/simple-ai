package harshal.temkar.ai.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-model pricing in USD per 1K tokens.
 * Ollama models are local => price = 0.
 * Anthropic prices are list prices (override via app.ai.pricing.* if needed).
 *
 * Key format: "<provider>:<model>" e.g. "anthropic:claude-3-5-sonnet-20241022"
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai.pricing")
public class ModelPricing {

    private Map<String, PricePer1K> models = defaults();

    public PricePer1K priceFor(String providerModelKey) {
        if (providerModelKey == null) return PricePer1K.zero();
        return models.getOrDefault(providerModelKey.toLowerCase(), PricePer1K.zero());
    }

    private static Map<String, PricePer1K> defaults() {
        Map<String, PricePer1K> m = new HashMap<>();
        // Ollama local => free
        m.put("ollama:llama3",                 PricePer1K.zero());
        m.put("ollama:qwen",                   PricePer1K.zero());
        // Anthropic Claude 3.5 Sonnet (list prices, USD / 1K tokens)
        m.put("anthropic:claude-3-5-sonnet-20241022", new PricePer1K(0.003d, 0.015d));
        return m;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricePer1K {
        private double inputUsd;
        private double outputUsd;

        public static PricePer1K zero() {
            return new PricePer1K(0.0d, 0.0d);
        }
    }
}
