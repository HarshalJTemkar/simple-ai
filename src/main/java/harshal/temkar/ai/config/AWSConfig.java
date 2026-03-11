package harshal.temkar.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AWSConfig {

	private final AWSProperty awsProperty;
	
	@Bean
	AwsCredentialsProvider credentialsProvider() {
		
		AwsCredentialsProvider provider;
		
		if (!awsProperty.getAccessKey().equalsIgnoreCase("ignore")
				&& !awsProperty.getSecretKey().equalsIgnoreCase("ignore")) {
			
			AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(awsProperty.getAccessKey(),
					awsProperty.getSecretKey(), awsProperty.getToken());
			provider = StaticCredentialsProvider.create(sessionCredentials);
			
		} else {
			
			provider = DefaultCredentialsProvider.builder().build();
		}

		AwsCredentials credentials = provider.resolveCredentials();
		
		log.info("Loaded AWS Credentials. {}",credentials);
		return provider;
	}
}
