package harshal.temkar.ai.model.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OllamaModel {
	
    LLAMA3("llama3.2:1b"),
    QWEN("qwen3.5:0.8b");
    
    private final String modelName;
}