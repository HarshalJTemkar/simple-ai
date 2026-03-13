package harshal.temkar.ai.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

	@Value("${server.servlet.context-path:/}")
	private String contextPath;

	@Bean
	OpenAPI customOpenAPI() {
		
		return new OpenAPI()
				.info(new Info()
						.title("Simple AI Chat API")
						.version("1.0.0")
						.description("Production-grade AI Chat Microservice with Spring AI")
						.contact(new Contact()
								.name("Harshal Temkar")
								.email("harshal@example.com"))
						.license(new License()
								.name("Apache 2.0")
								.url("https://www.apache.org/licenses/LICENSE-2.0.html")))
				.servers(List.of(new Server()
						.url("http://localhost:8090" + contextPath)
						.description("Local Development Server")));
	}
}