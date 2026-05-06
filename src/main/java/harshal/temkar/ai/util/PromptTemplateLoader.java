package harshal.temkar.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PromptTemplateLoader {

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public String loadTemplate(String templatePath) {
        if (templateCache.containsKey(templatePath)) {
            log.debug("Returning cached template: {}", templatePath);
            return templateCache.get(templatePath);
        }

        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            if (!resource.exists()) {
                log.error("Template not found: {}", templatePath);
                throw new IllegalArgumentException("Template not found: " + templatePath);
            }

            String content = Files.readString(Path.of(resource.getURI()));
            templateCache.put(templatePath, content);
            
            log.debug("Loaded template: {}", templatePath);
            return content;
            
        } catch (IOException e) {
            log.error("Failed to load template: {}", templatePath, e);
            throw new RuntimeException("Failed to load template: " + templatePath, e);
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

    public void clearCache() {
        log.info("Clearing template cache");
        templateCache.clear();
    }
}