package harshal.temkar.ai.controller.chat;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import harshal.temkar.ai.model.chat.ChatRequest;
import harshal.temkar.ai.model.chat.ChatResponse;
import harshal.temkar.ai.service.chat.IChatService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final IChatService chatService;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.ask(request);
    }
}
