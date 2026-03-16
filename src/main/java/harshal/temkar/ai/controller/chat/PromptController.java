package harshal.temkar.ai.controller.chat;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import harshal.temkar.ai.util.PromptTemplateLoader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
@Tag(name = "Prompt Management", description = "Manage RAG prompt templates")
public class PromptController {

    private final PromptTemplateLoader promptTemplateLoader;

    @GetMapping("/{templateName}")
    @Operation(summary = "Get prompt template", description = "Retrieve a specific prompt template")
    public ResponseEntity<String> getTemplate(@PathVariable String templateName) {
        log.info("Retrieving template: {}", templateName);
        String template = promptTemplateLoader.loadTemplate(templateName);
        return ResponseEntity.ok(template);
    }

    @PostMapping("/{templateName}/reload")
    @Operation(summary = "Reload prompt template", description = "Clear cache and reload template")
    public ResponseEntity<String> reloadTemplate(@PathVariable String templateName) {
        log.info("Reloading template: {}", templateName);
        promptTemplateLoader.reloadTemplate(templateName);
        return ResponseEntity.ok("Template reloaded successfully: " + templateName);
    }

    @PostMapping("/clear-cache")
    @Operation(summary = "Clear prompt cache", description = "Clear all cached prompt templates")
    public ResponseEntity<String> clearCache() {
        log.info("Clearing prompt template cache");
        promptTemplateLoader.clearCache();
        return ResponseEntity.ok("Prompt cache cleared successfully");
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview prompt with variables", 
               description = "Preview how a prompt will look with variable substitution")
    public ResponseEntity<String> previewPrompt(
            @RequestParam String templateName,
            @RequestBody Map<String, String> variables) {
        
        log.info("Previewing template: {} with variables", templateName);
        String preview = promptTemplateLoader.loadTemplate(templateName, variables);
        return ResponseEntity.ok(preview);
    }
}