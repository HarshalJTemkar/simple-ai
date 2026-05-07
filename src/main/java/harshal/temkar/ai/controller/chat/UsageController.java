package harshal.temkar.ai.controller.chat;

import harshal.temkar.ai.config.ModelPricing;
import harshal.temkar.ai.service.chat.TokenUsageTracker;
import harshal.temkar.ai.service.chat.TokenUsageTracker.Stats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Usage & Pricing", description = "Token usage, cost tracking and model pricing")
public class UsageController {

    private final TokenUsageTracker tracker;
    private final ModelPricing pricing;

    @GetMapping("/usage")
    @Operation(summary = "Aggregate token usage and cost (global + by model)")
    public Map<String, Object> usage() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("global", toMap(tracker.global()));
        out.put("byModel", tracker.byModel().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> toMap(e.getValue()))));
        return out;
    }

    @GetMapping("/usage/{sessionId}")
    @Operation(summary = "Token usage and cost for a session")
    public Map<String, Object> sessionUsage(@PathVariable String sessionId) {
        return toMap(tracker.forSession(sessionId));
    }

    @DeleteMapping("/usage")
    @Operation(summary = "Reset all in-memory usage counters")
    public Map<String, String> reset() {
        tracker.reset();
        return Map.of("status", "reset");
    }

    @GetMapping("/pricing")
    @Operation(summary = "Get configured per-model pricing (USD per 1K tokens)")
    public Map<String, ModelPricing.PricePer1K> pricing() {
        return pricing.getModels();
    }

    private Map<String, Object> toMap(Stats s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("calls", s.getCalls());
        m.put("cachedCalls", s.getCachedCalls());
        m.put("promptTokens", s.getPromptTokens());
        m.put("completionTokens", s.getCompletionTokens());
        m.put("totalTokens", s.getTotalTokens());
        m.put("costUsd", s.getCostUsd());
        return m;
    }
}
