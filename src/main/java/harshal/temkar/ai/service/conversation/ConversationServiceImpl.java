package harshal.temkar.ai.service.conversation;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import harshal.temkar.ai.config.ConversationCacheProperties;
import harshal.temkar.ai.config.PromptProperties;
import harshal.temkar.ai.model.conversation.ConversationContext;
import harshal.temkar.ai.model.conversation.ConversationMessage;
import harshal.temkar.ai.model.conversation.ConversationSummary;
import harshal.temkar.ai.repository.conversation.IConversationRepository;
import harshal.temkar.ai.service.constants.Constants;
import harshal.temkar.ai.util.PromptTemplateLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements IConversationService {

    private final IConversationRepository repository;
    private final ConversationCacheProperties properties;
    private final PromptProperties promptProperties;
    private final PromptTemplateLoader promptTemplateLoader;

    @Override
    public void saveMessage(String sessionId, ConversationMessage message) {
        log.debug("Saving message for session: {}", sessionId);

        ConversationContext context = repository.findBySessionId(sessionId)
                .orElseGet(() -> createNewContext(sessionId));

        // Enforce max messages limit
        if (context.getMessages() != null &&
            context.getMessages().size() >= properties.getMaxMessagesPerSession()) {
            context.getMessages().remove(0);
            log.debug("Removed oldest message to maintain limit for session: {}", sessionId);
        }

        context.addMessage(message);
        repository.save(sessionId, context);
    }

    @Override
    public Optional<ConversationContext> getContext(String sessionId) {
        return repository.findBySessionId(sessionId);
    }

    @Override
    public List<ConversationMessage> getRecentMessages(String sessionId, int limit) {
        return repository.findBySessionId(sessionId)
                .map(context -> context.getRecentMessages(limit))
                .orElse(List.of());
    }

    @Override
    public String buildPromptWithContext(String sessionId, String newMessage, int contextLimit) {
        List<ConversationMessage> recentMessages = getRecentMessages(sessionId, contextLimit);

        if (recentMessages.isEmpty()) {
            return newMessage;
        }

        String history = recentMessages.stream()
                .map(m -> m.getRole() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        Map<String, String> vars = new HashMap<>();
        vars.put(Constants.VAR_HISTORY, history);
        vars.put(Constants.VAR_USER, newMessage);

        String templatePath = promptProperties.getTemplates().getSystemPath()
                + promptProperties.getTemplates().getConversationContextTemplate();
        return promptTemplateLoader.loadTemplate(templatePath, vars);
    }

    @Override
    public void deleteConversation(String sessionId) {
        log.info("Deleting conversation: {}", sessionId);
        repository.deleteBySessionId(sessionId);
    }

    @Override
    public ConversationSummary getSummary(String sessionId) {
        return repository.findBySessionId(sessionId)
                .map(context -> ConversationSummary.builder()
                        .sessionId(sessionId)
                        .messageCount(context.getMessageCount())
                        .totalTokens(context.getTotalTokens())
                        .createdAt(context.getCreatedAt())
                        .lastAccessedAt(context.getLastAccessedAt())
                        .build())
                .orElse(null);
    }

    @Override
    public void clearAll() {
        log.warn("Clearing all conversations");
        repository.clear();
    }

    private ConversationContext createNewContext(String sessionId) {
        return ConversationContext.builder()
                .sessionId(sessionId)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .totalTokens(0)
                .messageCount(0)
                .build();
    }
}