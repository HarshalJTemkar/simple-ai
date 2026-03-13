package harshal.temkar.ai.model.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnthropicModel {
	
	CLAUDE_3_5_SONNET("claude-3-5-sonnet-20241022");

	private final String modelName;
}