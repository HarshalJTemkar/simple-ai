package harshal.temkar.ai.interceptor;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import harshal.temkar.ai.config.RateLimitCacheProperties;
import harshal.temkar.ai.config.RateLimitProperties;
import harshal.temkar.ai.exception.RateLimitException;
import harshal.temkar.ai.service.constants.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Lightweight per-IP fixed-window rate limiter, backed by Caffeine.
 * Throws {@link RateLimitException} (handled by GlobalExceptionHandler -> 429)
 * when a client exceeds {@code capacity} requests inside {@code windowSeconds}.
 *
 * <p>Note: this is intentionally simple (single-node, in-memory). For multi-node
 * deployments, swap to a Redis-based limiter (e.g. Bucket4j + Redis).
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitProperties properties;
    private final Cache<String, AtomicInteger> counters;

    public RateLimitInterceptor(RateLimitProperties properties, RateLimitCacheProperties cacheProperties) {
        this.properties = properties;
        this.counters = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(Math.max(1, properties.getWindowSeconds())))
                .maximumSize(cacheProperties.getMaxSize())
                .build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.isEnabled()) {
            return true;
        }
        String clientId = resolveClientId(request);
        AtomicInteger counter = counters.get(clientId, k -> new AtomicInteger(0));
        int used = counter.incrementAndGet();
        int capacity = properties.getCapacity();

        response.setHeader(Constants.HEADER_RATE_LIMIT_LIMIT, String.valueOf(capacity));
        response.setHeader(Constants.HEADER_RATE_LIMIT_REMAINING, String.valueOf(Math.max(0, capacity - used)));
        response.setHeader(Constants.HEADER_RATE_LIMIT_WINDOW, String.valueOf(properties.getWindowSeconds()));

        if (used > capacity) {
            log.warn("Rate limit exceeded for client {} ({}/{} in {}s window)",
                    clientId, used, capacity, properties.getWindowSeconds());
            response.setHeader(Constants.HEADER_RETRY_AFTER, String.valueOf(properties.getWindowSeconds()));
            throw new RateLimitException("Too many requests for client " + clientId
                    + ". Limit=" + capacity + " per " + properties.getWindowSeconds() + "s");
        }
        return true;
    }

    private String resolveClientId(HttpServletRequest request) {
        String fwd = request.getHeader(Constants.HEADER_FORWARDED_FOR);
        if (fwd != null && !fwd.isBlank()) {
            int comma = fwd.indexOf(',');
            return (comma > 0 ? fwd.substring(0, comma) : fwd).trim();
        }
        String real = request.getHeader(Constants.HEADER_REAL_IP);
        if (real != null && !real.isBlank()) {
            return real.trim();
        }
        return request.getRemoteAddr();
    }
}
