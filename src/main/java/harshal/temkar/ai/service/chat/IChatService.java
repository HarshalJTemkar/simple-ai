package harshal.temkar.ai.service.chat;

import harshal.temkar.ai.model.chat.ChatRequest;
import harshal.temkar.ai.model.chat.ChatResponse;

public interface IChatService {
	
	ChatResponse ask(ChatRequest request);
}
