package me.ferreira.graveto.portfolio.positions.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.portfolio.BrokerNotFoundException;
import me.ferreira.graveto.common.web.exception.portfolio.InsufficientPermissionsOnBrokerException;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.portfolio.positions.service.PositionService;
import me.ferreira.graveto.portfolio.positions.service.command.FetchPortfolioOverviewCommand;
import me.ferreira.graveto.portfolio.positions.service.payload.PortfolioSummary;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(
    controllers = PositionController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class FetchPortfolioValuationControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private PositionService service;

  @Test
  void shouldFetchPortfolioValuationSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();

    final PortfolioSummary summary = new PortfolioSummary(
        BigDecimal.valueOf(100), BigDecimal.valueOf(1000), BigDecimal.valueOf(800), BigDecimal.valueOf(80)
    );

    final ArgumentCaptor<FetchPortfolioOverviewCommand> commandCaptor =
        ArgumentCaptor.forClass(FetchPortfolioOverviewCommand.class);
    when(service.generatePortfolioValuationOverview(commandCaptor.capture())).thenReturn(summary);

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/brokers/" + brokerSid + "/positions/summary")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);

    final FetchPortfolioOverviewCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.brokerSid()).isEqualTo(brokerSid);

    assertThat(result).bodyJson().extractingPath("$.totalInvested").asNumber()
        .isEqualTo(BigDecimal.valueOf(100).intValue());
    assertThat(result).bodyJson().extractingPath("$.totalMarketValue").asNumber()
        .isEqualTo(BigDecimal.valueOf(1000).intValue());
    assertThat(result).bodyJson().extractingPath("$.totalUnrealizedPnL").asNumber()
        .isEqualTo(BigDecimal.valueOf(800).intValue());
    assertThat(result).bodyJson().extractingPath("$.totalUnrealizedPnlPercent").asNumber()
        .isEqualTo(BigDecimal.valueOf(80).intValue());
  }

  @Test
  void shouldReturnNotFoundWhenBrokerDoesNotExist() {
    // Arrange
    final UUID brokerSid = UUID.randomUUID();

    when(service.generatePortfolioValuationOverview(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new BrokerNotFoundException(brokerSid));

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/brokers/" + brokerSid + "/positions/summary")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturnForbiddenWhenUserLacksPermission() {
    // Arrange
    when(service.generatePortfolioValuationOverview(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new InsufficientPermissionsOnBrokerException("request portfolio valuation overview"));

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/brokers/" + UUID.randomUUID() + "/positions/summary")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void shouldReturnBadRequestForInvalidBrokerSid() {
    // Act
    final MvcTestResult result = mvc.get()
        .uri("/brokers/not-a-uuid/positions/summary")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
  }

}
