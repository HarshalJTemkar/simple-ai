package harshal.temkar.ai.conversation.repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import harshal.temkar.ai.config.ConversationCacheProperties;
import harshal.temkar.ai.conversation.model.ConversationContext;
import harshal.temkar.ai.exception.AiException;
import harshal.temkar.ai.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

@Slf4j
@Repository
@ConditionalOnProperty(prefix = "app.conversation.cache", name = "type", havingValue = "dynamodb")
@RequiredArgsConstructor
public class DynamoDBConversationRepository implements IConversationRepository {

	private final DynamoDbClient dynamoDbClient;
	private final ConversationCacheProperties properties;
	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Override
	public void save(String sessionId, ConversationContext context) {
		log.debug("Saving conversation to DynamoDB: {}", sessionId);

		try {
			Map<String, AttributeValue> item = new HashMap<>();
			item.put("sessionId", AttributeValue.builder().s(sessionId).build());
			item.put("data", AttributeValue.builder().s(objectMapper.writeValueAsString(context)).build());
			item.put("ttl", AttributeValue.builder().n(String.valueOf(calculateTTL())).build());
			item.put("createdAt", AttributeValue.builder().s(context.getCreatedAt().toString()).build());
			item.put("lastAccessedAt", AttributeValue.builder().s(context.getLastAccessedAt().toString()).build());
			item.put("messageCount", AttributeValue.builder()
					.n(String.valueOf(context.getMessageCount() != null ? context.getMessageCount() : 0)).build());

			PutItemRequest request = PutItemRequest.builder().tableName(properties.getDynamodbTableName()).item(item)
					.build();

			dynamoDbClient.putItem(request);
			log.debug("Successfully saved conversation to DynamoDB: {}", sessionId);

		} catch (JsonProcessingException e) {
			log.error("Failed to serialize conversation context for session: {}", sessionId, e);
			throw new AiException(ErrorCode.INTERNAL_ERROR, "Failed to save conversation", e);
		} catch (DynamoDbException e) {
			log.error("DynamoDB error while saving conversation: {}", sessionId, e);
			throw new AiException(ErrorCode.INTERNAL_ERROR, "DynamoDB operation failed: " + e.getMessage(), e);
		}
	}

	@Override
	public Optional<ConversationContext> findBySessionId(String sessionId) {
		log.debug("Retrieving conversation from DynamoDB: {}", sessionId);

		try {
			Map<String, AttributeValue> key = new HashMap<>();
			key.put("sessionId", AttributeValue.builder().s(sessionId).build());

			GetItemRequest request = GetItemRequest.builder().tableName(properties.getDynamodbTableName()).key(key)
					.build();

			GetItemResponse response = dynamoDbClient.getItem(request);

			if (!response.hasItem() || response.item().isEmpty()) {
				log.debug("No conversation found in DynamoDB for session: {}", sessionId);
				return Optional.empty();
			}

			String data = response.item().get("data").s();
			ConversationContext context = objectMapper.readValue(data, ConversationContext.class);
			log.debug("Successfully retrieved conversation from DynamoDB: {}", sessionId);

			return Optional.of(context);

		} catch (JsonProcessingException e) {
			log.error("Failed to deserialize conversation context for session: {}", sessionId, e);
			return Optional.empty();
		} catch (DynamoDbException e) {
			log.error("DynamoDB error while retrieving conversation: {}", sessionId, e);
			return Optional.empty();
		}
	}

	@Override
	public void deleteBySessionId(String sessionId) {
		log.debug("Deleting conversation from DynamoDB: {}", sessionId);

		try {
			Map<String, AttributeValue> key = new HashMap<>();
			key.put("sessionId", AttributeValue.builder().s(sessionId).build());

			DeleteItemRequest request = DeleteItemRequest.builder().tableName(properties.getDynamodbTableName())
					.key(key).build();

			dynamoDbClient.deleteItem(request);
			log.debug("Successfully deleted conversation from DynamoDB: {}", sessionId);

		} catch (DynamoDbException e) {
			log.error("DynamoDB error while deleting conversation: {}", sessionId, e);
			throw new AiException(ErrorCode.INTERNAL_ERROR, "Failed to delete conversation: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean exists(String sessionId) {
		return findBySessionId(sessionId).isPresent();
	}

	@Override
	public void clear() {
		log.warn("Clear all operation for DynamoDB requires table scan - not recommended for production");

		try {
			// Scan table to get all session IDs
			ScanRequest scanRequest = ScanRequest.builder().tableName(properties.getDynamodbTableName())
					.projectionExpression("sessionId").build();

			ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

			// Delete each item
			for (Map<String, AttributeValue> item : scanResponse.items()) {
				String sessionId = item.get("sessionId").s();
				deleteBySessionId(sessionId);
			}

			log.info("Cleared {} conversations from DynamoDB", scanResponse.count());

		} catch (DynamoDbException e) {
			log.error("Failed to clear conversations from DynamoDB", e);
			throw new AiException(ErrorCode.INTERNAL_ERROR, "Failed to clear conversations: " + e.getMessage(), e);
		}
	}

	@Override
	public String getCacheType() {
		return "dynamodb";
	}

	private long calculateTTL() {
		return Instant.now().getEpochSecond() + (properties.getTtlMinutes() * 60L);
	}
}