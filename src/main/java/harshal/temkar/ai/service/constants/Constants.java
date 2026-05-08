package harshal.temkar.ai.service.constants;

/**
 * Centralized application-wide constants.
 *
 * <p>Holds <strong>structural</strong>, non-tunable string/numeric literals that should
 * never be hardcoded inline (HTTP header names, cache names, role labels, metadata keys,
 * template placeholder names, log prefixes, etc.).</p>
 *
 * <p>All <em>tunable</em> values (sizes, timeouts, thresholds, prompts, paths, ratios)
 * live in {@code application.yaml} and are bound through {@code @ConfigurationProperties}
 * classes – not here.</p>
 */
public final class Constants {

    private Constants() { /* no instances */ }

    // ==================== HTTP HEADERS ====================
    public static final String HEADER_CORRELATION_ID       = "X-Correlation-ID";
    public static final String HEADER_FORWARDED_FOR        = "X-Forwarded-For";
    public static final String HEADER_REAL_IP              = "X-Real-IP";
    public static final String HEADER_RATE_LIMIT_LIMIT     = "X-RateLimit-Limit";
    public static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RATE_LIMIT_WINDOW    = "X-RateLimit-Window-Seconds";
    public static final String HEADER_RETRY_AFTER          = "Retry-After";

    // ==================== MDC KEYS ====================
    public static final String MDC_CORRELATION_ID = "correlationId";

    // ==================== CONVERSATION ROLES ====================
    public static final String ROLE_USER      = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM    = "system";

    // ==================== AI PROVIDER NAMES (lowercase keys used in registries) ====================
    public static final String PROVIDER_OLLAMA    = "ollama";
    public static final String PROVIDER_ANTHROPIC = "anthropic";
    public static final String PROVIDER_UNKNOWN   = "unknown";

    // ==================== CHUNKING STRATEGIES ====================
    public static final String CHUNK_STRATEGY_SENTENCE  = "sentence";
    public static final String CHUNK_STRATEGY_PARAGRAPH = "paragraph";
    public static final String CHUNK_STRATEGY_FIXED     = "fixed";

    // ==================== VECTOR / DOCUMENT METADATA KEYS ====================
    public static final String META_DOCUMENT_ID = "documentId";
    public static final String META_FILENAME    = "filename";
    public static final String META_CHUNK_INDEX = "chunkIndex";
    public static final String META_DISTANCE    = "distance";

    // ==================== UPLOAD STATUS ====================
    public static final String UPLOAD_STATUS_SUCCESS = "SUCCESS";
    public static final String UPLOAD_SUCCESS_MESSAGE = "Document uploaded and indexed successfully";

    // ==================== CACHE NAMES ====================
    public static final String CACHE_CHAT_RESPONSES = "chatResponses";
    public static final String CACHE_MODEL_INFO     = "modelInfo";
    public static final String CACHE_SYSTEM_PROMPTS = "systemPrompts";
    public static final String CACHE_MANAGER_RESPONSE = "responseCacheManager";

    // ==================== EXECUTOR / BEAN NAMES ====================
    public static final String BEAN_AI_TASK_EXECUTOR = "aiTaskExecutor";

    // ==================== TEMPLATE PLACEHOLDER NAMES ====================
    public static final String VAR_QUERY     = "query";
    public static final String VAR_CONTEXT   = "context";
    public static final String VAR_SYSTEM    = "system";
    public static final String VAR_HISTORY   = "history";
    public static final String VAR_USER      = "user";
    public static final String VAR_ROLE      = "role";
    public static final String VAR_CONTENT   = "content";
    public static final String VAR_FILENAME  = "filename";
    public static final String VAR_SOURCE_ID = "id";

    // ==================== TEMPLATE FILE NAMES (under prompt template dirs) ====================
    public static final String TEMPLATE_DEFAULT_SYSTEM       = "default.txt";
    public static final String TEMPLATE_PROMPT_BUILDER       = "prompt-builder.txt";
    public static final String TEMPLATE_CONVERSATION_CONTEXT = "conversation-context.txt";
    public static final String TEMPLATE_RAG_SOURCE_BLOCK     = "source-block.txt";
    public static final String TEMPLATE_TXT_EXT              = ".txt";

    // ==================== JSON / API ====================
    public static final String API_BASE_PATH        = "/api/**";
    public static final String DEFAULT_PROVIDER_KEY_SEPARATOR = ":";

    // ==================== MISC ====================
    public static final String EMPTY_STRING = "";
}
