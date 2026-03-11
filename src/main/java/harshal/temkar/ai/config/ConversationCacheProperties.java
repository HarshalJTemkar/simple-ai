package harshal.temkar.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.conversation.cache")
public class ConversationCacheProperties {
    
    private String type = "local"; // local, redis, dynamodb
    private Integer maxSize = 1000; // Max conversations to cache
    private Integer ttlMinutes = 60; // Time to live in minutes
    private Integer maxMessagesPerSession = 50; // Max messages per conversation
    
    // Redis specific
    private String redisKeyPrefix = "conversation:";
    
    // DynamoDB specific
    private String dynamodbTableName = "conversations";
    private String daxEndpoint; // Optional DAX endpoint
}