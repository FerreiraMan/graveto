package me.ferreira.graveto.portfolio.assets.client.impl;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.common.ExternalApiUnavailableException;
import me.ferreira.graveto.common.web.exception.portfolio.client.AssetInvalidRequestException;
import me.ferreira.graveto.common.web.exception.portfolio.client.QuoteDataInvalidRequestException;
import me.ferreira.graveto.portfolio.assets.client.MarketDataClient;
import me.ferreira.graveto.portfolio.assets.client.impl.dto.response.QuoteDataResponseDto;
import me.ferreira.graveto.portfolio.assets.client.impl.dto.response.SearchAssetResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class YahooFinanceClient implements MarketDataClient {

  private final RestClient restClient;

  public YahooFinanceClient(final RestClient.Builder restClientBuilder,
                            final @Value("${http.client.yahoo.base-url}") String baseUri,
                            final @Value("${http.client.yahoo.api-key:}") String apiKey) {
    this.restClient = restClientBuilder
        .baseUrl(baseUri)
        .defaultHeader("x-api-key", apiKey)
        .build();
  }

  @Override
  public List<SearchAssetResponseDto.Result> searchAsset(final UUID userSid, final String keyword) {

    final SearchAssetResponseDto response = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("v6/finance/autocomplete")
            .queryParam("query", keyword.toLowerCase().trim())
            .build())
        .attribute("userSid", userSid)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
          throw new AssetInvalidRequestException(keyword, res.getStatusCode().value());
        })
        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
          throw new ExternalApiUnavailableException(res.getStatusCode().value());
        })
        .body(SearchAssetResponseDto.class);


    return response != null ? response.resultSet().result() : List.of();
  }

  @Override
  public List<QuoteDataResponseDto.Result> fetchQuoteData(final UUID userSid, final List<String> symbols) {

    final String concatenatedSymbols = String.join(",", symbols);

    final QuoteDataResponseDto response = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("v6/finance/quote")
            .queryParam("symbols", concatenatedSymbols)
            .build())
        .attribute("userSid", userSid)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
          throw new QuoteDataInvalidRequestException(concatenatedSymbols, res.getStatusCode().value());
        })
        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
          throw new ExternalApiUnavailableException(res.getStatusCode().value());
        })
        .body(QuoteDataResponseDto.class);

    return response != null ? response.quoteResponse().result() : List.of();
  }

}
