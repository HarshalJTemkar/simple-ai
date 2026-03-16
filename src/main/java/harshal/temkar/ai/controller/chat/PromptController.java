package harshal.temkar.ai.controller.chat;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import harshal.temkar.ai.model.chat.OptimizedPrompt;
import harshal.temkar.ai.model.chat.PromptContext;
import harshal.temkar.ai.model.chat.PromptRole;
import harshal.temkar.ai.service.prompt.IPromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
@Tag(name = "Prompt Engineering", description = "Prompt management and optimization")
public class PromptController {

    private final IPromptService promptService;

    @GetMapping("/system/{role}")
    @Operation(summary = "Get system prompt", description = "Retrieve system prompt for a specific role")
    public ResponseEntity<String> getSystemPrompt(@PathVariable PromptRole role) {
        log.info("Fetching system prompt for role: {}", role);
        return ResponseEntity.ok(promptService.getSystemPrompt(role));
    }

    @PostMapping("/optimize")
    @Operation(summary = "Optimize prompt", description = "Optimize a prompt for token efficiency")
    public ResponseEntity<OptimizedPrompt> optimizePrompt(
            @RequestParam(required = false) String systemPrompt,
            @RequestParam String userPrompt) {
        
        log.info("Optimizing prompt");
        return ResponseEntity.ok(promptService.optimizePrompt(systemPrompt, userPrompt));
    }

    @PostMapping("/build")
    @Operation(summary = "Build optimized prompt", description = "Build and optimize a complete prompt")
    public ResponseEntity<OptimizedPrompt> buildPrompt(@RequestBody PromptContext context) {
        log.info("Building prompt for role: {}", context.getRole());
        return ResponseEntity.ok(promptService.buildOptimizedPrompt(context));
    }
}