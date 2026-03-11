package harshal.temkar.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "aws")
@Data
public class AWSProperty {

	private String accessKey;

	private String secretKey;

	private String token;

	private String region;
}
