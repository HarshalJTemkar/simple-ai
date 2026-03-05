package harshal.temkar.ai.service.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import harshal.temkar.ai.exception.AiException;
import harshal.temkar.ai.exception.ErrorCode;
import harshal.temkar.ai.model.chat.ChatRequest;
import harshal.temkar.ai.model.chat.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements IChatService {

	private final ChatClient chatClient;

	@Override
	public ChatResponse ask(
			ChatRequest request) {
		
		try {
			log.debug("Processing chat request for session: {}", request.getSessionId());

			String response = chatClient
					.prompt()
					.user(request.getMessage())
					.call()
					.content();

			log.debug("AI response generated successfully for session: {}", request.getSessionId());

			return new ChatResponse(response, request.getSessionId());

		} catch (Exception ex) {
			
			log.error("Failed to process chat request for session: {}", request.getSessionId(), ex);
			throw new AiException(ErrorCode.AI_SERVICE_ERROR, "Failed to communicate with AI service", ex);
		}
	}
}