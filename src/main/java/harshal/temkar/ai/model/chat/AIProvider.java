package harshal.temkar.ai.model.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AIProvider {
    OLLAMA("ollama"),
    ANTHROPIC("anthropic");
    
    private final String providerName;
}