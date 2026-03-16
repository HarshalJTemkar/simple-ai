package harshal.temkar.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class PromptTemplateLoader {

    private final Map<String, String> templateCache = new HashMap<>();
    private static final String TEMPLATE_BASE_PATH = "templates/prompts/";

    /**
     * Load a prompt template from resources
     */
    public String loadTemplate(String templateName) {
        // Check cache first
        if (templateCache.containsKey(templateName)) {
            log.debug("Returning cached template: {}", templateName);
            return templateCache.get(templateName);
        }

        try {
            String templatePath = TEMPLATE_BASE_PATH + templateName;
            ClassPathResource resource = new ClassPathResource(templatePath);
            
            if (!resource.exists()) {
                log.error("Template not found: {}", templatePath);
                throw new IllegalArgumentException("Prompt template not found: " + templateName);
            }

            String content = Files.readString(Path.of(resource.getURI()));
            
            // Cache the template
            templateCache.put(templateName, content);
            
            log.debug("Loaded and cached template: {}", templateName);
            return content;
            
        } catch (IOException e) {
            log.error("Failed to load prompt template: {}", templateName, e);
            throw new RuntimeException("Failed to load prompt template: " + templateName, e);
        }
    }

    /**
     * Load template with variable substitution
     */
    public String loadTemplate(String templateName, Map<String, String> variables) {
        String template = loadTemplate(templateName);
        
        // Replace placeholders with actual values
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            template = template.replace(placeholder, entry.getValue());
        }
        
        return template;
    }

    /**
     * Clear template cache (useful for development/testing)
     */
    public void clearCache() {
        log.info("Clearing prompt template cache");
        templateCache.clear();
    }

    /**
     * Reload specific template
     */
    public void reloadTemplate(String templateName) {
        log.info("Reloading template: {}", templateName);
        templateCache.remove(templateName);
        loadTemplate(templateName);
    }
}