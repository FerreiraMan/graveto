package me.ferreira.graveto.portfolio.assets.client.impl.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "http.client.yahoo")
public record YahooFinanceProperties(
    String name,
    String baseUrl,
    String apiKey,
    SearchProperties search,
    QuoteProperties quote
) {
  public record SearchProperties(String path) {
  }

  public record QuoteProperties(String path, int batchLimit) {
  }
}
