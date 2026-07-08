package me.ferreira.graveto.portfolio.positions.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.portfolio.BrokerNotFoundException;
import me.ferreira.graveto.common.web.exception.portfolio.InsufficientPermissionsOnBrokerException;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.portfolio.positions.service.PositionService;
import me.ferreira.graveto.portfolio.positions.service.command.FetchPositionOverviewCommand;
import me.ferreira.graveto.portfolio.positions.service.payload.PositionValuation;
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
public class FetchPositionValuationControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private PositionService service;

  @Test
  void shouldFetchPositionValuationsSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();
    final UUID assetSid = UUID.randomUUID();

    final PositionValuation valuation = new PositionValuation(
        assetSid, "IWDA",
        new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("727"),
        new BigDecimal("89.45"), new BigDecimal("894.50"),
        new BigDecimal("167.50"), new BigDecimal("23.0399")
    );

    final ArgumentCaptor<FetchPositionOverviewCommand> commandCaptor =
        ArgumentCaptor.forClass(FetchPositionOverviewCommand.class);
    when(service.generatePositionValuationOverview(commandCaptor.capture())).thenReturn(List.of(valuation));

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/brokers/" + brokerSid + "/positions")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);

    final FetchPositionOverviewCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.brokerSid()).isEqualTo(brokerSid);

    assertThat(result).bodyJson().extractingPath("$[0].assetSid").asString().isEqualTo(assetSid.toString());
    assertThat(result).bodyJson().extractingPath("$[0].ticker").asString().isEqualTo("IWDA");
  }

  @Test
  void shouldReturnEmptyListWhenNoPositionsExist() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID brokerSid = UUID.randomUUID();

    when(service.generatePositionValuationOverview(org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of());

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/brokers/" + brokerSid + "/positions")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(result).bodyJson().extractingPath("$").asList().isEmpty();
  }

  @Test
  void shouldReturnNotFoundWhenBrokerDoesNotExist() {
    // Arrange
    final UUID brokerSid = UUID.randomUUID();

    when(service.generatePositionValuationOverview(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new BrokerNotFoundException(brokerSid));

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/brokers/" + brokerSid + "/positions")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturnForbiddenWhenUserLacksPermission() {
    // Arrange
    when(service.generatePositionValuationOverview(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new InsufficientPermissionsOnBrokerException("request valuation overview"));

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/brokers/" + UUID.randomUUID() + "/positions")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void shouldReturnBadRequestForInvalidBrokerSid() {
    // Act
    final MvcTestResult result = mvc.get()
        .uri("/brokers/not-a-uuid/positions")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
  }

}
