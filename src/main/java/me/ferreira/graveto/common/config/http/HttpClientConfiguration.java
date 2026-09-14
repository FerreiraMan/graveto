package me.ferreira.graveto.common.config.http;

import java.time.Duration;
import me.ferreira.graveto.common.config.properties.HttpProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfiguration {

  private final HttpProperties httpProperties;
  private final RequestLoggerInterceptor loggerInterceptor;
  private final RequestRateLimiterInterceptor rateLimiterInterceptor;

  public HttpClientConfiguration(final HttpProperties httpProperties, final RequestLoggerInterceptor loggerInterceptor,
                                 final RequestRateLimiterInterceptor rateLimiterInterceptor) {
    this.httpProperties = httpProperties;
    this.loggerInterceptor = loggerInterceptor;
    this.rateLimiterInterceptor = rateLimiterInterceptor;
  }

  @Bean
  public RestClient.Builder restClientBuilder() {

    final SimpleClientHttpRequestFactory simple = new SimpleClientHttpRequestFactory();
    simple.setConnectTimeout(Duration.ofMillis(httpProperties.client().connectTimeout()));
    simple.setReadTimeout(Duration.ofMillis(httpProperties.client().readTimeout()));
    final BufferingClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(simple);

    return RestClient.builder()
        .requestFactory(factory)
        .configureMessageConverters(HttpMessageConverters.Builder::registerDefaults)
        .requestInterceptor(rateLimiterInterceptor)
        .requestInterceptor(loggerInterceptor);
  }

}
