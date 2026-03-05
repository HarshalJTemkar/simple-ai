package harshal.temkar.ai.controller.chat;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import harshal.temkar.ai.model.chat.ChatRequest;
import harshal.temkar.ai.model.chat.ChatResponse;
import harshal.temkar.ai.service.chat.IChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat API", description = "AI Chat Operations")
public class ChatController {

	private final IChatService chatService;

	@PostMapping(
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Send chat message", 
			description = "Send a message to AI and receive response")
	public ChatResponse chat(
			@Valid @RequestBody ChatRequest request,
			@RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

		log.info("Received chat request. CorrelationId: {}, SessionId: {}", correlationId, request.getSessionId());
		return chatService.ask(request);
	}
}