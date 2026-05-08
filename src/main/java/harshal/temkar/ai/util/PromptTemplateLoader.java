package harshal.temkar.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PromptTemplateLoader {

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public String loadTemplate(String templatePath) {
        String cached = templateCache.get(templatePath);
        if (cached != null) {
            log.debug("Returning cached template: {}", templatePath);
            return cached;
        }

        String resolved = resolvePath(templatePath);
        ClassPathResource resource = new ClassPathResource(resolved);
        if (!resource.exists()) {
            log.error("Template not found: {}", resolved);
            throw new IllegalArgumentException("Template not found: " + resolved);
        }

        try (InputStream in = resource.getInputStream()) {
            String content = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            templateCache.put(templatePath, content);
            log.debug("Loaded template: {}", resolved);
            return content;
        } catch (IOException e) {
            log.error("Failed to load template: {}", resolved, e);
            throw new RuntimeException("Failed to load template: " + resolved, e);
        }
    }

    public String loadTemplate(String templatePath, Map<String, String> variables) {
        String template = loadTemplate(templatePath);
        return replaceVariables(template, variables);
    }

    private String replaceVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /**
     * Allow callers to pass either a bare filename (e.g. "rag-prompt.txt") or a
     * fully-qualified classpath location (e.g. "templates/prompts/rag/rag-prompt.txt").
     */
    private String resolvePath(String templatePath) {
        if (templatePath == null || templatePath.isBlank()) {
            throw new IllegalArgumentException("Template path must not be blank");
        }
        if (templatePath.contains("/")) {
            return templatePath;
        }
        // Default lookup directory for bare filenames
        return "templates/prompts/rag/" + templatePath;
    }

    public void clearCache() {
        log.info("Clearing template cache");
        templateCache.clear();
    }
}