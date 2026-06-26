package me.ferreira.graveto.common.infrastructure.http;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfiguration {

  @Value("${http.client.connect-timeout:5000}")
  int connectTimeout;

  @Value("${http.client.read-timeout:10000}")
  int readTimeout;

  private final RequestLoggerInterceptor loggerInterceptor;
  private final RequestRateLimiterInterceptor rateLimiterInterceptor;

  public HttpClientConfiguration(final RequestLoggerInterceptor loggerInterceptor,
                                 final RequestRateLimiterInterceptor rateLimiterInterceptor) {
    this.loggerInterceptor = loggerInterceptor;
    this.rateLimiterInterceptor = rateLimiterInterceptor;
  }

  @Bean
  public RestClient.Builder restClientBuilder() {

    final SimpleClientHttpRequestFactory simple = new SimpleClientHttpRequestFactory();
    simple.setConnectTimeout(Duration.ofMillis(connectTimeout));
    simple.setReadTimeout(Duration.ofMillis(readTimeout));
    final BufferingClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(simple);

    return RestClient.builder()
        .requestFactory(factory)
        .configureMessageConverters(HttpMessageConverters.Builder::registerDefaults)
        .requestInterceptor(rateLimiterInterceptor)
        .requestInterceptor(loggerInterceptor);
  }

}
