package harshal.temkar.ai.service.prompt;

import org.springframework.stereotype.Service;

import harshal.temkar.ai.model.chat.OptimizedPrompt;
import harshal.temkar.ai.model.chat.PromptContext;
import harshal.temkar.ai.model.chat.PromptRole;
import harshal.temkar.ai.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptServiceImpl implements IPromptService {

    private final PromptBuilder promptBuilder;
    private final PromptOptimizer promptOptimizer;

    @Override
    public OptimizedPrompt buildOptimizedPrompt(PromptContext context) {
        log.debug("Building optimized prompt for role: {}", context.getRole());
        
        String fullPrompt = promptBuilder.buildPrompt(context);
        
        // Extract system and user parts for optimization
        String systemPrompt = context.getSystemPrompt() != null ? 
            context.getSystemPrompt() : "";
        
        String userPrompt = context.getUserMessage();
        
        return promptOptimizer.optimize(systemPrompt, userPrompt);
    }

    @Override
    public String getSystemPrompt(PromptRole role) {
        PromptContext context = PromptContext.builder()
                .role(role)
                .userMessage("")
                .build();
        
        return promptBuilder.buildPrompt(context);
    }

    @Override
    public OptimizedPrompt optimizePrompt(String systemPrompt, String userPrompt) {
        return promptOptimizer.optimize(systemPrompt, userPrompt);
    }
}
