package harshal.temkar.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Tunables for the {@link harshal.temkar.ai.service.constants.Constants#BEAN_AI_TASK_EXECUTOR}
 * thread-pool task executor.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.async")
public class AsyncProperties {

    private int corePoolSize = 5;
    private int maxPoolSize = 10;
    private int queueCapacity = 25;
    private String threadNamePrefix = "ai-async-";
    private boolean waitForTasksToCompleteOnShutdown = true;
    private int awaitTerminationSeconds = 60;
}
