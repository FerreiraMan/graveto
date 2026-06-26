package me.ferreira.graveto.portfolio.config;

import org.mockserver.client.MockServerClient;
import org.mockserver.testcontainers.MockServerContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class PortfolioIntegrationTestConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
  }

  @Bean
  MockServerContainer mockServerContainer() {
    final MockServerContainer container = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:7.2.0"));
    container.start();
    return container;
  }

  @Bean
  MockServerClient mockServerClient(final MockServerContainer container) {
    return new MockServerClient(container.getHost(), container.getServerPort());
  }

  @Bean
  static DynamicPropertyRegistrar mockServerProperties(final MockServerContainer container) {
    return registry -> {
      registry.add("http.client.yahoo.base-url", container::getEndpoint);
    };
  }

}
