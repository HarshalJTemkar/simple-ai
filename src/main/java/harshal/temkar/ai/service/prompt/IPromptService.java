package harshal.temkar.ai.service.prompt;

import harshal.temkar.ai.model.chat.OptimizedPrompt;
import harshal.temkar.ai.model.chat.PromptContext;
import harshal.temkar.ai.model.chat.PromptRole;

public interface IPromptService {
    
    OptimizedPrompt buildOptimizedPrompt(PromptContext context);
    
    String getSystemPrompt(PromptRole role);
    
    OptimizedPrompt optimizePrompt(String systemPrompt, String userPrompt);
}
