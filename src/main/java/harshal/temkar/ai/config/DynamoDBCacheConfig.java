package harshal.temkar.ai.config;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "app.conversation.cache", name = "type", havingValue = "dynamodb")
@RequiredArgsConstructor
public class DynamoDBCacheConfig {

	private final ConversationCacheProperties properties;
	private final AwsCredentialsProvider credentialsProvider; // Inject your custom provider
	private final AWSProperty awsProperty; // Inject AWS properties

	@Bean
	DynamoDbClient dynamoDbClient() {
		log.info("Initializing DynamoDB client for region: {}", awsProperty.getRegion());

		DynamoDbClientBuilder builder = DynamoDbClient.builder().region(Region.of(awsProperty.getRegion()))
				.credentialsProvider(credentialsProvider); // Use your custom credentials provider

		// Use DAX if endpoint provided
		if (properties.getDaxEndpoint() != null && !properties.getDaxEndpoint().isEmpty()) {
			log.info("Using DAX endpoint: {}", properties.getDaxEndpoint());
			builder.endpointOverride(URI.create(properties.getDaxEndpoint()));
		}

		DynamoDbClient client = builder.build();
		log.info("DynamoDB client initialized successfully");

		return client;
	}

	@Bean
	DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
		log.info("Initializing DynamoDB Enhanced client");
		return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
	}
}