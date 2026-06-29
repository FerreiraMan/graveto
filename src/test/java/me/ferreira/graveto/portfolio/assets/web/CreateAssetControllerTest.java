package me.ferreira.graveto.portfolio.assets.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.UUID;
import java.util.stream.Stream;
import me.ferreira.graveto.common.domain.Country;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.domain.AssetType;
import me.ferreira.graveto.portfolio.assets.service.AssetService;
import me.ferreira.graveto.portfolio.assets.service.command.CreateAssetCommand;
import me.ferreira.graveto.portfolio.assets.web.dto.request.CreateAssetRequestDto;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(
    controllers = AssetController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class CreateAssetControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private AssetService service;

  private static Stream<Arguments> invalidCreateAssetRequests() {
    return Stream.of(
        Arguments.of(new CreateAssetRequestDto(null, "iShares", AssetType.ETF, Currency.EUR), "symbol"),
        Arguments.of(new CreateAssetRequestDto("  ", "iShares", AssetType.ETF, Currency.EUR), "symbol"),
        Arguments.of(new CreateAssetRequestDto("IWDA.AS", null, AssetType.ETF, Currency.EUR), "name"),
        Arguments.of(new CreateAssetRequestDto("IWDA.AS", "  ", AssetType.ETF, Currency.EUR), "name"),
        Arguments.of(new CreateAssetRequestDto("IWDA.AS", "iShares", null, Currency.EUR), "type"),
        Arguments.of(new CreateAssetRequestDto("IWDA.AS", "iShares", AssetType.ETF, null), "currency")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidCreateAssetRequests")
  void shouldReturnBadRequestForInvalidPayloads(
      final CreateAssetRequestDto request,
      final String expectedErrorField) {

    final MvcTestResult result = mvc.post()
        .uri("/assets")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .hasPath("$.invalid_params." + expectedErrorField);
  }

  @Test
  void shouldCreateAssetSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID assetSid = UUID.randomUUID();
    final CreateAssetRequestDto request = new CreateAssetRequestDto(
        "IWDA.AS", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);

    final Country country = new Country();
    country.setName("Netherlands");

    final StockExchange exchange = new StockExchange();
    exchange.setSuffix(".AS");
    exchange.setMarket("Euronext Amsterdam");
    exchange.setCountry(country);

    final Asset mockAsset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    mockAsset.setSid(assetSid);
    mockAsset.setStockExchange(exchange);

    final ArgumentCaptor<CreateAssetCommand> commandCaptor = ArgumentCaptor.forClass(CreateAssetCommand.class);
    when(service.createAsset(commandCaptor.capture())).thenReturn(mockAsset);

    // Act
    final MvcTestResult result = mvc.post()
        .uri("/assets")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.CREATED);
    assertThat(result).hasHeader("Location", "http://localhost/assets/" + assetSid);

    final CreateAssetCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.symbol()).isEqualTo("IWDA.AS");
    assertThat(captured.name()).isEqualTo("iShares Core MSCI World");
    assertThat(captured.type()).isEqualTo(AssetType.ETF);
    assertThat(captured.currency()).isEqualTo(Currency.EUR);

    assertThat(result).bodyJson().extractingPath("$.sid").asString().isEqualTo(assetSid.toString());
    assertThat(result).bodyJson().extractingPath("$.ticker").asString().isEqualTo("IWDA");
    assertThat(result).bodyJson().extractingPath("$.name").asString().isEqualTo("iShares Core MSCI World");
    assertThat(result).bodyJson().extractingPath("$.type").asString().isEqualTo("ETF");
    assertThat(result).bodyJson().extractingPath("$.currency").asString().isEqualTo("EUR");
    assertThat(result).bodyJson().extractingPath("$.exchangeLocation").asString().isEqualTo("Netherlands");
    assertThat(result).bodyJson().extractingPath("$.exchangeName").asString().isEqualTo("Euronext Amsterdam");
  }

  @Test
  void shouldTrimSymbolBeforePassingToService() {
    // Arrange
    final CreateAssetRequestDto request = new CreateAssetRequestDto(
        "  IWDA.AS  ", "iShares", AssetType.ETF, Currency.EUR);

    final Country country = new Country();
    country.setName("Netherlands");

    final StockExchange exchange = new StockExchange();
    exchange.setMarket("Euronext Amsterdam");
    exchange.setCountry(country);

    final Asset mockAsset = Asset.create("IWDA", "iShares", AssetType.ETF, Currency.EUR);
    mockAsset.setStockExchange(exchange);

    final ArgumentCaptor<CreateAssetCommand> commandCaptor = ArgumentCaptor.forClass(CreateAssetCommand.class);
    when(service.createAsset(commandCaptor.capture())).thenReturn(mockAsset);

    // Act
    mvc.post()
        .uri("/assets")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(commandCaptor.getValue().symbol()).isEqualTo("IWDA.AS");
  }

}
