package harshal.temkar.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "app.prompt")
public class PromptProperties {
    
    private boolean enabled;
    private String defaultRole;
    private Integer maxPromptLength;
    private Optimization optimization = new Optimization();
    private Templates templates = new Templates();
    
    @Data
    public static class Optimization {
        private boolean enabled;
        private boolean trimWhitespace;
        private boolean removeEmptyLines;
        private Integer maxSystemPromptLength = 2000;
        private Integer maxContextLength = 6000;
    }
    
    @Data
    public static class Templates {
        private String systemPath = "templates/prompts/system/";
        private String taskPath = "templates/prompts/task/";
        private String ragPath = "templates/prompts/rag/";
        private Map<String, String> customTemplates = new HashMap<>();
    }
}