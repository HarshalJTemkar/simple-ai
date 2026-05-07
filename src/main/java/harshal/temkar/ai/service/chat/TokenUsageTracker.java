package harshal.temkar.ai.service.chat;

import harshal.temkar.ai.util.TokenUsage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * In-memory aggregator for token usage and cost.
 * Tracks totals globally, per model, and per session.
 */
@Slf4j
@Component
public class TokenUsageTracker {

    private final Stats global = new Stats();
    private final Map<String, Stats> byModel   = new ConcurrentHashMap<>();
    private final Map<String, Stats> bySession = new ConcurrentHashMap<>();

    public void record(String sessionId, TokenUsage usage) {
        if (usage == null) return;
        long pt = nz(usage.getPromptTokens());
        long ct = nz(usage.getCompletionTokens());
        double cost = usage.getEstimatedCostUsd() == null ? 0.0d : usage.getEstimatedCostUsd();
        boolean cached = Boolean.TRUE.equals(usage.getCached());

        global.add(pt, ct, cost, cached);
        if (usage.getModel() != null) {
            byModel.computeIfAbsent(usage.getModel(), k -> new Stats()).add(pt, ct, cost, cached);
        }
        if (sessionId != null && !sessionId.isBlank()) {
            bySession.computeIfAbsent(sessionId, k -> new Stats()).add(pt, ct, cost, cached);
        }
    }

    public Stats global()                       { return global; }
    public Map<String, Stats> byModel()         { return byModel; }
    public Stats forSession(String sessionId)   { return bySession.getOrDefault(sessionId, new Stats()); }

    public void reset() {
        global.reset(); byModel.clear(); bySession.clear();
    }

    private static long nz(Integer v) { return v == null ? 0L : v.longValue(); }

    @Data
    public static class Stats {
        private final LongAdder calls            = new LongAdder();
        private final LongAdder cachedCalls      = new LongAdder();
        private final LongAdder promptTokens     = new LongAdder();
        private final LongAdder completionTokens = new LongAdder();
        private final DoubleAdder costUsd        = new DoubleAdder();

        void add(long pt, long ct, double cost, boolean cached) {
            calls.increment();
            if (cached) cachedCalls.increment();
            promptTokens.add(pt);
            completionTokens.add(ct);
            costUsd.add(cost);
        }
        void reset() {
            calls.reset(); cachedCalls.reset();
            promptTokens.reset(); completionTokens.reset();
            costUsd.reset();
        }
        public long getCalls()             { return calls.sum(); }
        public long getCachedCalls()       { return cachedCalls.sum(); }
        public long getPromptTokens()      { return promptTokens.sum(); }
        public long getCompletionTokens()  { return completionTokens.sum(); }
        public long getTotalTokens()       { return getPromptTokens() + getCompletionTokens(); }
        public double getCostUsd()         { return costUsd.sum(); }
    }
}
