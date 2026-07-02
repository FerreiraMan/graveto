package me.ferreira.graveto.portfolio.assets.client.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.common.ExternalApiUnavailableException;
import me.ferreira.graveto.common.web.exception.portfolio.client.AssetInvalidRequestException;
import me.ferreira.graveto.common.web.exception.portfolio.client.QuoteDataInvalidRequestException;
import me.ferreira.graveto.portfolio.assets.client.MarketDataClient;
import me.ferreira.graveto.portfolio.assets.client.impl.config.YahooFinanceProperties;
import me.ferreira.graveto.portfolio.assets.client.impl.dto.response.QuoteDataResponseDto;
import me.ferreira.graveto.portfolio.assets.client.impl.dto.response.SearchAssetResponseDto;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class YahooFinanceClient implements MarketDataClient {

  private final RestClient restClient;
  private final YahooFinanceProperties yahooFinanceProperties;

  public YahooFinanceClient(final RestClient.Builder restClientBuilder,
                            final YahooFinanceProperties yahooFinanceProperties) {

    this.restClient = restClientBuilder
        .baseUrl(yahooFinanceProperties.baseUrl())
        .defaultHeader("x-api-key", yahooFinanceProperties.apiKey())
        .build();
    this.yahooFinanceProperties = yahooFinanceProperties;
  }

  @Override
  public List<SearchAssetResponseDto.Result> searchAsset(final UUID userSid, final String keyword) {

    final SearchAssetResponseDto response = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(yahooFinanceProperties.search().path())
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

    final int batchLimit = yahooFinanceProperties.quote().batchLimit();
    final int nrOfRequests = (int) Math.ceil((double) symbols.size() / batchLimit);

    final List<QuoteDataResponseDto.Result> resultList = new ArrayList<>();

    for (int i = 0; i < nrOfRequests; i++) {

      final int fromIndex = i * batchLimit;
      final int toIndex = Math.min(fromIndex + batchLimit, symbols.size());
      final List<String> partitionedSymbols = symbols.subList(fromIndex, toIndex);

      final String concatenatedSymbols = String.join(",", partitionedSymbols);

      final QuoteDataResponseDto response = restClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(yahooFinanceProperties.quote().path())
              .queryParam("symbols", concatenatedSymbols)
              .build())
          .attribute("userSid", userSid != null ? userSid : "")
          .retrieve()
          .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
            throw new QuoteDataInvalidRequestException(concatenatedSymbols, res.getStatusCode().value());
          })
          .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
            throw new ExternalApiUnavailableException(res.getStatusCode().value());
          })
          .body(QuoteDataResponseDto.class);

      if (response != null) {
        resultList.addAll(response.quoteResponse().result());
      }
    }

    return resultList;
  }

}
