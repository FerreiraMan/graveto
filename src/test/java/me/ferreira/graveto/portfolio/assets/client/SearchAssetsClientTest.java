package me.ferreira.graveto.portfolio.assets.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.common.ExternalApiUnavailableException;
import me.ferreira.graveto.common.web.exception.portfolio.client.AssetInvalidRequestException;
import me.ferreira.graveto.portfolio.assets.client.impl.YahooFinanceClient;
import me.ferreira.graveto.portfolio.assets.client.impl.dto.response.SearchAssetResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

public class SearchAssetsClientTest {

  private YahooFinanceClient yahooFinanceClient;
  private MockRestServiceServer mockServer;

  @BeforeEach
  void setup() {
    final RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    yahooFinanceClient = new YahooFinanceClient(builder, "http://localhost", "");
  }

  @Test
  void shouldReturnParsedResults() {
    // Arrange
    final String responseBody = """
        {"ResultSet":{"Query":"iwda","Result":[
          {"symbol":"IWDA.AS","name":"iShares Core MSCI World","exch":"AMS","type":"E","exchDisp":"Amsterdam","typeDisp":"ETF"},
          {"symbol":"IWDA.L","name":"iShares Core MSCI World","exch":"LSE","type":"E","exchDisp":"London","typeDisp":"ETF"}
        ]}}
        """;

    mockServer.expect(requestTo("http://localhost/v6/finance/autocomplete?query=iwda"))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    // Act
    final List<SearchAssetResponseDto.Result> results = yahooFinanceClient.searchAsset(UUID.randomUUID(), "IWDA");

    // Assert
    assertThat(results).hasSize(2);
    assertThat(results.getFirst().symbol()).isEqualTo("IWDA.AS");
    assertThat(results.getFirst().name()).isEqualTo("iShares Core MSCI World");
    assertThat(results.getFirst().exchange()).isEqualTo("Amsterdam");
    assertThat(results.getFirst().type()).isEqualTo("ETF");
    assertThat(results.get(1).symbol()).isEqualTo("IWDA.L");

    mockServer.verify();
  }

  @Test
  void shouldReturnEmptyListWhenNoResults() {
    // Arrange
    final String responseBody = """
        {"ResultSet":{"Query":"xyz","Result":[]}}
        """;

    mockServer.expect(requestTo("http://localhost/v6/finance/autocomplete?query=xyz"))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    // Act
    final List<SearchAssetResponseDto.Result> results = yahooFinanceClient.searchAsset(UUID.randomUUID(), "xyz");

    // Assert
    assertThat(results).isEmpty();
    mockServer.verify();
  }

  @Test
  void shouldThrowAssetInvalidRequestExceptionOn4xx() {
    // Arrange
    mockServer.expect(requestTo("http://localhost/v6/finance/autocomplete?query=bad"))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST));

    // Act & Assert
    assertThatThrownBy(() -> yahooFinanceClient.searchAsset(UUID.randomUUID(), "bad"))
        .isInstanceOf(AssetInvalidRequestException.class);

    mockServer.verify();
  }

  @Test
  void shouldThrowExternalApiUnavailableExceptionOn5xx() {
    // Arrange
    mockServer.expect(requestTo("http://localhost/v6/finance/autocomplete?query=iwda"))
        .andRespond(withServerError());

    // Act & Assert
    assertThatThrownBy(() -> yahooFinanceClient.searchAsset(UUID.randomUUID(), "iwda"))
        .isInstanceOf(ExternalApiUnavailableException.class);

    mockServer.verify();
  }

  @Test
  void shouldTrimAndLowercaseKeyword() {
    // Arrange
    final String responseBody = """
        {"ResultSet":{"Query":"iwda","Result":[]}}
        """;

    mockServer.expect(requestTo("http://localhost/v6/finance/autocomplete?query=iwda"))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    // Act
    yahooFinanceClient.searchAsset(UUID.randomUUID(), "  IWDA  ");

    // Assert
    mockServer.verify();
  }

}
