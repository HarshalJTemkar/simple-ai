package harshal.temkar.ai.service.chat;

import harshal.temkar.ai.model.chat.ChatRequest;
import harshal.temkar.ai.model.chat.ChatResponse;
import harshal.temkar.ai.model.chat.StreamingChatResponse;
import reactor.core.publisher.Flux;

public interface IChatService {
	
	ChatResponse ask(ChatRequest request);
	
	// Streaming chat (new)
    Flux<StreamingChatResponse> askStreaming(ChatRequest request);
}
