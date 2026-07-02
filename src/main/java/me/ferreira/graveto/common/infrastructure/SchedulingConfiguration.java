package me.ferreira.graveto.common.infrastructure;

import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.TaskUtils;

@Configuration
@EnableScheduling
public class SchedulingConfiguration implements ThreadPoolTaskSchedulerCustomizer {

  @Override
  public void customize(final ThreadPoolTaskScheduler taskScheduler) {
    taskScheduler.setThreadNamePrefix("scheduler-");
    taskScheduler.setErrorHandler(TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER);
  }

}
