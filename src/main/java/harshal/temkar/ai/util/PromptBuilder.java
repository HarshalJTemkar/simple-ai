package harshal.temkar.ai.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import harshal.temkar.ai.config.ChatProperties;
import harshal.temkar.ai.config.PromptProperties;
import harshal.temkar.ai.model.chat.PromptContext;
import harshal.temkar.ai.model.chat.PromptRole;
import harshal.temkar.ai.service.constants.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private final PromptTemplateLoader templateLoader;
    private final PromptProperties promptProperties;
    private final ChatProperties chatProperties;

    public String buildPrompt(PromptContext context) {
        String systemPrompt = (context.getSystemPrompt() != null && !context.getSystemPrompt().isEmpty())
                ? context.getSystemPrompt()
                : (context.getRole() != null ? loadSystemPrompt(context.getRole()) : Constants.EMPTY_STRING);

        String history = (context.getConversationHistory() != null && !context.getConversationHistory().isEmpty())
                ? context.getConversationHistory()
                : Constants.EMPTY_STRING;

        Map<String, String> vars = new HashMap<>();
        vars.put(Constants.VAR_SYSTEM, systemPrompt == null ? Constants.EMPTY_STRING : systemPrompt);
        vars.put(Constants.VAR_HISTORY, history);
        vars.put(Constants.VAR_USER, context.getUserMessage() == null ? Constants.EMPTY_STRING : context.getUserMessage());

        String templatePath = promptProperties.getTemplates().getSystemPath()
                + promptProperties.getTemplates().getPromptBuilderTemplate();
        return templateLoader.loadTemplate(templatePath, vars);
    }

    private String loadSystemPrompt(PromptRole role) {
        String templatePath = promptProperties.getTemplates().getSystemPath()
                + role.getRoleName() + Constants.TEMPLATE_TXT_EXT;
        try {
            return templateLoader.loadTemplate(templatePath);
        } catch (Exception e) {
            log.warn("Failed to load system prompt for role: {}, using default template", role);
            return loadDefaultSystemPrompt();
        }
    }

    private String loadDefaultSystemPrompt() {
        String defaultPath = promptProperties.getTemplates().getSystemPath()
                + promptProperties.getTemplates().getDefaultSystemTemplate();
        try {
            return templateLoader.loadTemplate(defaultPath);
        } catch (Exception e) {
            log.warn("Failed to load default system template, using configured fallback string");
            return chatProperties.getDefaultSystemPromptFallback();
        }
    }

    public String buildWithVariables(String templatePath, Map<String, String> variables) {
        return templateLoader.loadTemplate(templatePath, variables);
    }
}