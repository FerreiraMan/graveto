package me.ferreira.graveto.common.config;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.common.config.properties.ThreadPoolExecutorProperties;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

  private final ThreadPoolExecutorProperties executorProperties;

  public AsyncConfiguration(final ThreadPoolExecutorProperties executorProperties) {
    this.executorProperties = executorProperties;
  }

  @Override
  public Executor getAsyncExecutor() {
    return taskExecutor();
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return (throwable, method, params) ->
      log.error("Exception in async method: {}", method.getName(), throwable);
  }

  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(executorProperties.poolSize());
    executor.setMaxPoolSize(executorProperties.maxPoolSize());
    executor.setKeepAliveSeconds(executorProperties.keepAliveSeconds());
    executor.setQueueCapacity(executorProperties.queueCapacity());
    executor.setThreadNamePrefix("async-task-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(executorProperties.awaitTerminationSeconds());
    executor.initialize();
    return executor;
  }

}
