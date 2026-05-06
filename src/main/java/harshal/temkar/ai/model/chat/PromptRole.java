package harshal.temkar.ai.model.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PromptRole {
	
    ASSISTANT("assistant", "General purpose assistant"),
    CODE_ASSISTANT("code-assistant", "Code generation and explanation"),
    ANALYST("analyst", "Data analysis and insights"),
    TRANSLATOR("translator", "Language translation"),
    SUMMARIZER("summarizer", "Text summarization"),
    CUSTOM("custom", "Custom role");
    
    private final String roleName;
    private final String description;
}