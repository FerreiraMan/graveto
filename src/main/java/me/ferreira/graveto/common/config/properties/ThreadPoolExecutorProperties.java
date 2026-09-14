package me.ferreira.graveto.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "async-executor")
public record ThreadPoolExecutorProperties(
    Integer poolSize,
    Integer maxPoolSize,
    Integer keepAliveSeconds,
    Integer queueCapacity,
    Integer awaitTerminationSeconds
) {
}
