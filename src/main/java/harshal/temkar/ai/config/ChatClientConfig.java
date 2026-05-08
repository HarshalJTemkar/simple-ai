package harshal.temkar.ai.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import harshal.temkar.ai.service.constants.Constants;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ChatClientConfig {

	@Bean
	@Primary
	@Qualifier("ollamaChatClient")
	ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel) {
		log.info("Initializing Ollama ChatClient");
		return ChatClient.builder(ollamaChatModel).build();
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.ai.anthropic", name = "api-key")
	@Qualifier("anthropicChatClient")
	ChatClient anthropicChatClient(AnthropicChatModel anthropicChatModel) {
		log.info("Initializing Anthropic (Claude) ChatClient");
		return ChatClient.builder(anthropicChatModel).build();
	}

	// ==================== CHAT CLIENT REGISTRY ====================

	@Bean
	Map<String, ChatClient> chatClientRegistry(
			@Qualifier("ollamaChatClient") ChatClient ollamaChatClient,
			@Qualifier("anthropicChatClient") ChatClient anthropicChatClient) {

		Map<String, ChatClient> registry = new HashMap<>();

		// Ollama is always registered
		registry.put(Constants.PROVIDER_OLLAMA, ollamaChatClient);
		log.info("Registered Ollama ChatClient");

		// Anthropic only if API key provided
		if (anthropicChatClient != null) {
			registry.put(Constants.PROVIDER_ANTHROPIC, anthropicChatClient);
			log.info("Registered Anthropic ChatClient");
		}

		log.info("ChatClient Registry initialized with {} providers", registry.size());
		return registry;
	}
}