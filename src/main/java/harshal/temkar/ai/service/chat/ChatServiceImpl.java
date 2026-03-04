package harshal.temkar.ai.service.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

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
	public ChatResponse ask(ChatRequest request) {
		String response = chatClient
				.prompt()
                .user(request.getMessage())
                .call()
                .content();

        return new ChatResponse(response);
	}
}
