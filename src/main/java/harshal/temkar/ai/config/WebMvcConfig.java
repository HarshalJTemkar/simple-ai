package harshal.temkar.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import harshal.temkar.ai.interceptor.CorrelationIdInterceptor;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

	private final CorrelationIdInterceptor correlationIdInterceptor;

	@Override
	public void addInterceptors(
			InterceptorRegistry registry) {
		
		registry.addInterceptor(correlationIdInterceptor);
	}
}