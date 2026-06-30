package me.ferreira.graveto.portfolio.assets.client.impl.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QuoteDataResponseDto(
    QuoteResponse quoteResponse
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record QuoteResponse(
      List<Result> result
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Result(
      String symbol,
      String longName,
      String quoteType,
      BigDecimal regularMarketPrice,
      BigDecimal regularMarketDayHigh,
      BigDecimal regularMarketDayLow,
      BigDecimal regularMarketOpen,
      BigDecimal regularMarketPreviousClose,
      String currency,
      BigDecimal fiftyTwoWeekLow,
      BigDecimal fiftyTwoWeekHigh,
      BigDecimal fiftyTwoWeekChangePercent
  ) {
  }
}