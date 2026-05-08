package harshal.temkar.ai.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import harshal.temkar.ai.service.constants.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class PerformanceConfig {

    private final AsyncProperties asyncProperties;

    @Bean(name = Constants.BEAN_AI_TASK_EXECUTOR)
    Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getCorePoolSize());
        executor.setMaxPoolSize(asyncProperties.getMaxPoolSize());
        executor.setQueueCapacity(asyncProperties.getQueueCapacity());
        executor.setThreadNamePrefix(asyncProperties.getThreadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(asyncProperties.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(asyncProperties.getAwaitTerminationSeconds());
        executor.initialize();

        log.info("Initialized AI Task Executor - CorePool: {}, MaxPool: {}, Queue: {}",
                asyncProperties.getCorePoolSize(), asyncProperties.getMaxPoolSize(),
                asyncProperties.getQueueCapacity());
        return executor;
    }
}