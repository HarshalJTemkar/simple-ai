package harshal.temkar.ai.controller.chat;

import harshal.temkar.ai.model.conversation.ConversationContext;
import harshal.temkar.ai.model.conversation.ConversationSummary;
import harshal.temkar.ai.service.conversation.IConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversation API", description = "Conversation history management")
public class ConversationController {

    private final IConversationService conversationService;

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get conversation context", description = "Retrieve full conversation history")
    public ResponseEntity<ConversationContext> getConversation(@PathVariable String sessionId) {
        log.info("Retrieving conversation: {}", sessionId);
        return conversationService.getContext(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{sessionId}/summary")
    @Operation(summary = "Get conversation summary", description = "Retrieve conversation metadata")
    public ResponseEntity<ConversationSummary> getSummary(@PathVariable String sessionId) {
        log.info("Retrieving conversation summary: {}", sessionId);
        ConversationSummary summary = conversationService.getSummary(sessionId);
        return summary != null ? ResponseEntity.ok(summary) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Delete conversation", description = "Remove conversation from cache")
    public ResponseEntity<Void> deleteConversation(@PathVariable String sessionId) {
        log.info("Deleting conversation: {}", sessionId);
        conversationService.deleteConversation(sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Clear all conversations", description = "Remove all conversations from cache")
    public ResponseEntity<Void> clearAll() {
        log.warn("Clearing all conversations");
        conversationService.clearAll();
        return ResponseEntity.noContent().build();
    }
}
