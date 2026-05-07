package harshal.temkar.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import harshal.temkar.ai.interceptor.CorrelationIdInterceptor;
import harshal.temkar.ai.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

	private final CorrelationIdInterceptor correlationIdInterceptor;
	private final RateLimitInterceptor rateLimitInterceptor;
	private final RateLimitProperties rateLimitProperties;
	private final CorsProperties corsProperties;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(correlationIdInterceptor);

		if (rateLimitProperties.isEnabled() && !rateLimitProperties.getPaths().isEmpty()) {
			registry.addInterceptor(rateLimitInterceptor)
					.addPathPatterns(rateLimitProperties.getPaths());
		}
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOriginPatterns(corsProperties.getAllowedOrigins().toArray(new String[0]))
				.allowedMethods(corsProperties.getAllowedMethods().toArray(new String[0]))
				.allowedHeaders(corsProperties.getAllowedHeaders().toArray(new String[0]))
				.allowCredentials(corsProperties.isAllowCredentials())
				.maxAge(corsProperties.getMaxAge());
	}
}