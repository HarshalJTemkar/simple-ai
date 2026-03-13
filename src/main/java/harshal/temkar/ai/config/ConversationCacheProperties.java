package harshal.temkar.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.conversation.cache")
public class ConversationCacheProperties {
    
    private Integer maxSize;
    private Integer ttlMinutes;
    private Integer maxMessagesPerSession;
}