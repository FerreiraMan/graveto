package me.ferreira.graveto.portfolio.assets.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.common.ExternalApiUnavailableException;
import me.ferreira.graveto.common.web.exception.portfolio.client.QuoteDataInvalidRequestException;
import me.ferreira.graveto.portfolio.assets.client.impl.YahooFinanceClient;
import me.ferreira.graveto.portfolio.assets.client.impl.config.YahooFinanceProperties;
import me.ferreira.graveto.portfolio.assets.client.impl.dto.response.QuoteDataResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

public class FetchQuoteDataClientTest {

  private YahooFinanceClient yahooFinanceClient;
  private MockRestServiceServer mockServer;
  private final YahooFinanceProperties properties = new YahooFinanceProperties(
      "http://localhost",
      "",
      new YahooFinanceProperties.SearchProperties("v6/finance/autocomplete"),
      new YahooFinanceProperties.QuoteProperties("v6/finance/quote", 3)
  );

  @BeforeEach
  void setup() {
    final RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    yahooFinanceClient = new YahooFinanceClient(builder, properties);
  }

  @Test
  void shouldReturnParsedQuoteResults() {
    // Arrange
    final String responseBody = """
        {"quoteResponse":{"result":[
          {"symbol":"IWDA.AS","longName":"iShares Core MSCI World","quoteType":"ETF","regularMarketPrice":89.45,"regularMarketDayHigh":90.10,"regularMarketDayLow":88.80,"regularMarketOpen":89.00,"regularMarketPreviousClose":89.20,"currency":"EUR","fiftyTwoWeekLow":72.50,"fiftyTwoWeekHigh":91.30,"fiftyTwoWeekChangePercent":15.2}
        ]}}
        """;

    mockServer.expect(requestTo("http://localhost/v6/finance/quote?symbols=IWDA.AS"))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    // Act
    final List<QuoteDataResponseDto.Result> results =
        yahooFinanceClient.fetchQuoteData(UUID.randomUUID(), List.of("IWDA.AS"));

    // Assert
    assertThat(results).hasSize(1);
    assertThat(results.getFirst().symbol()).isEqualTo("IWDA.AS");
    assertThat(results.getFirst().quoteType()).isEqualTo("ETF");
    assertThat(results.getFirst().regularMarketPrice()).isEqualByComparingTo(new BigDecimal("89.45"));
    assertThat(results.getFirst().currency()).isEqualTo("EUR");
    assertThat(results.getFirst().longName()).isEqualTo("iShares Core MSCI World");

    mockServer.verify();
  }

  @Test
  void shouldReturnMultipleQuotes() {
    // Arrange
    final String responseBody = """
        {"quoteResponse":{"result":[
          {"symbol":"IWDA.AS","longName":"iShares Core MSCI World","quoteType":"ETF","regularMarketPrice":89.45,"currency":"EUR"},
          {"symbol":"VWCE.DE","longName":"Vanguard FTSE All-World","quoteType":"ETF","regularMarketPrice":105.20,"currency":"EUR"}
        ]}}
        """;

    mockServer.expect(requestTo("http://localhost/v6/finance/quote?symbols=IWDA.AS,VWCE.DE"))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    // Act
    final List<QuoteDataResponseDto.Result> results =
        yahooFinanceClient.fetchQuoteData(UUID.randomUUID(), List.of("IWDA.AS", "VWCE.DE"));

    // Assert
    assertThat(results).hasSize(2);
    assertThat(results.get(0).symbol()).isEqualTo("IWDA.AS");
    assertThat(results.get(1).symbol()).isEqualTo("VWCE.DE");

    mockServer.verify();
  }

  @Test
  void shouldReturnEmptyListWhenNoResults() {
    // Arrange
    final String responseBody = """
        {"quoteResponse":{"result":[]}}
        """;

    mockServer.expect(requestTo("http://localhost/v6/finance/quote?symbols=FAKE.XX"))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    // Act
    final List<QuoteDataResponseDto.Result> results =
        yahooFinanceClient.fetchQuoteData(UUID.randomUUID(), List.of("FAKE.XX"));

    // Assert
    assertThat(results).isEmpty();
    mockServer.verify();
  }

  @Test
  void shouldThrowQuoteDataInvalidRequestExceptionOn4xx() {
    // Arrange
    mockServer.expect(requestTo("http://localhost/v6/finance/quote?symbols=BAD"))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST));

    // Act & Assert
    assertThatThrownBy(() -> yahooFinanceClient.fetchQuoteData(UUID.randomUUID(), List.of("BAD")))
        .isInstanceOf(QuoteDataInvalidRequestException.class);

    mockServer.verify();
  }

  @Test
  void shouldThrowExternalApiUnavailableExceptionOn5xx() {
    // Arrange
    mockServer.expect(requestTo("http://localhost/v6/finance/quote?symbols=IWDA.AS"))
        .andRespond(withServerError());

    // Act & Assert
    assertThatThrownBy(() -> yahooFinanceClient.fetchQuoteData(UUID.randomUUID(), List.of("IWDA.AS")))
        .isInstanceOf(ExternalApiUnavailableException.class);

    mockServer.verify();
  }

  @Test
  void shouldConcatenateMultipleSymbolsWithComma() {
    // Arrange
    final String responseBody = """
        {"quoteResponse":{"result":[]}}
        """;

    mockServer.expect(requestTo("http://localhost/v6/finance/quote?symbols=IWDA.AS,VWCE.DE,SXR8.DE"))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    // Act
    yahooFinanceClient.fetchQuoteData(UUID.randomUUID(), List.of("IWDA.AS", "VWCE.DE", "SXR8.DE"));

    // Assert
    mockServer.verify();
  }

  @Test
  void shouldBatchRequestsWhenSymbolsExceedLimit() {
    // Arrange — batch limit is 3, sending 5 symbols = 2 requests
    final String firstBatchResponse = """
        {"quoteResponse":{"result":[
          {"symbol":"A.AS","longName":"A","quoteType":"ETF","regularMarketPrice":10.0,"currency":"EUR"},
          {"symbol":"B.AS","longName":"B","quoteType":"ETF","regularMarketPrice":20.0,"currency":"EUR"},
          {"symbol":"C.AS","longName":"C","quoteType":"ETF","regularMarketPrice":30.0,"currency":"EUR"}
        ]}}
        """;
    final String secondBatchResponse = """
        {"quoteResponse":{"result":[
          {"symbol":"D.AS","longName":"D","quoteType":"ETF","regularMarketPrice":40.0,"currency":"EUR"},
          {"symbol":"E.AS","longName":"E","quoteType":"ETF","regularMarketPrice":50.0,"currency":"EUR"}
        ]}}
        """;

    mockServer.expect(requestTo("http://localhost/v6/finance/quote?symbols=A.AS,B.AS,C.AS"))
        .andRespond(withSuccess(firstBatchResponse, MediaType.APPLICATION_JSON));
    mockServer.expect(requestTo("http://localhost/v6/finance/quote?symbols=D.AS,E.AS"))
        .andRespond(withSuccess(secondBatchResponse, MediaType.APPLICATION_JSON));

    // Act
    final List<QuoteDataResponseDto.Result> results = yahooFinanceClient.fetchQuoteData(
        UUID.randomUUID(), List.of("A.AS", "B.AS", "C.AS", "D.AS", "E.AS"));

    // Assert
    assertThat(results).hasSize(5);
    assertThat(results.get(0).symbol()).isEqualTo("A.AS");
    assertThat(results.get(4).symbol()).isEqualTo("E.AS");
    mockServer.verify();
  }

  @Test
  void shouldHandleExactBatchLimitWithoutExtraRequest() {
    // Arrange — batch limit is 3, sending exactly 3 symbols = 1 request
    final String responseBody = """
        {"quoteResponse":{"result":[
          {"symbol":"A.AS","longName":"A","quoteType":"ETF","regularMarketPrice":10.0,"currency":"EUR"},
          {"symbol":"B.AS","longName":"B","quoteType":"ETF","regularMarketPrice":20.0,"currency":"EUR"},
          {"symbol":"C.AS","longName":"C","quoteType":"ETF","regularMarketPrice":30.0,"currency":"EUR"}
        ]}}
        """;

    mockServer.expect(requestTo("http://localhost/v6/finance/quote?symbols=A.AS,B.AS,C.AS"))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    // Act
    final List<QuoteDataResponseDto.Result> results = yahooFinanceClient.fetchQuoteData(
        UUID.randomUUID(), List.of("A.AS", "B.AS", "C.AS"));

    // Assert
    assertThat(results).hasSize(3);
    mockServer.verify();
  }

  @Test
  void shouldReturnEmptyListWhenSymbolListIsEmpty() {
    // Act
    final List<QuoteDataResponseDto.Result> results = yahooFinanceClient.fetchQuoteData(UUID.randomUUID(), List.of());

    // Assert
    assertThat(results).isEmpty();
    mockServer.verify();
  }

}
