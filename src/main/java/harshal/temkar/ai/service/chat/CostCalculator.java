package harshal.temkar.ai.service.chat;

import harshal.temkar.ai.config.ModelPricing;
import harshal.temkar.ai.config.ModelPricing.PricePer1K;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CostCalculator {

    private final ModelPricing pricing;

    /**
     * @param providerModelKey e.g. "ollama:llama3" or "anthropic:claude-3-5-sonnet-20241022"
     * @return cost in USD; 0 for unknown / local models
     */
    public double calculate(String providerModelKey, int promptTokens, int completionTokens) {
        PricePer1K p = pricing.priceFor(providerModelKey);
        double cost = (promptTokens / 1000.0d) * p.getInputUsd()
                    + (completionTokens / 1000.0d) * p.getOutputUsd();
        log.debug("Cost for {} = ${} (in:{} out:{})", providerModelKey, cost, promptTokens, completionTokens);
        return cost;
    }
}
