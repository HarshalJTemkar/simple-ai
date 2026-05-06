package harshal.temkar.ai.util;

import java.util.Map;

import org.springframework.stereotype.Component;

import harshal.temkar.ai.config.PromptProperties;
import harshal.temkar.ai.model.chat.PromptContext;
import harshal.temkar.ai.model.chat.PromptRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private final PromptTemplateLoader templateLoader;
    private final PromptProperties promptProperties;

    public String buildPrompt(PromptContext context) {
        StringBuilder prompt = new StringBuilder();
        
        // Add system prompt if provided
        if (context.getSystemPrompt() != null && !context.getSystemPrompt().isEmpty()) {
            prompt.append(context.getSystemPrompt()).append("\n\n");
        } else if (context.getRole() != null) {
            // Load system prompt based on role
            String systemPrompt = loadSystemPrompt(context.getRole());
            prompt.append(systemPrompt).append("\n\n");
        }
        
        // Add conversation history if provided
        if (context.getConversationHistory() != null && !context.getConversationHistory().isEmpty()) {
            prompt.append("### Previous Conversation:\n")
                  .append(context.getConversationHistory())
                  .append("\n\n");
        }
        
        // Add user message
        prompt.append("### User Request:\n")
              .append(context.getUserMessage());
        
        return prompt.toString();
    }

    private String loadSystemPrompt(PromptRole role) {
        String templatePath = promptProperties.getTemplates().getSystemPath() + 
                            role.getRoleName() + ".txt";
        
        try {
            return templateLoader.loadTemplate(templatePath);
        } catch (Exception e) {
            log.warn("Failed to load system prompt for role: {}, using default", role);
            return getDefaultSystemPrompt();
        }
    }

    private String getDefaultSystemPrompt() {
        return "You are a helpful AI assistant. Provide accurate, concise, and helpful responses.";
    }

    public String buildWithVariables(String templatePath, Map<String, String> variables) {
        return templateLoader.loadTemplate(templatePath, variables);
    }
}