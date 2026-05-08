package harshal.temkar.ai.interceptor;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import harshal.temkar.ai.service.constants.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CorrelationIdInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(
			HttpServletRequest request, 
			HttpServletResponse response, 
			Object handler) {
		
		String correlationId = request.getHeader(Constants.HEADER_CORRELATION_ID);

		if (correlationId == null || correlationId.isBlank()) {
			correlationId = UUID.randomUUID().toString();
		}

		MDC.put(Constants.MDC_CORRELATION_ID, correlationId);
		response.setHeader(Constants.HEADER_CORRELATION_ID, correlationId);

		return true;
	}

	@Override
	public void afterCompletion(
			HttpServletRequest request, 
			HttpServletResponse response, 
			Object handler,
			Exception ex) {
		
		MDC.remove(Constants.MDC_CORRELATION_ID);
	}
}