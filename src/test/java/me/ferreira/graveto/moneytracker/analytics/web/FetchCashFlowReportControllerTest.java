package me.ferreira.graveto.moneytracker.analytics.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.analytics.service.AnalyticService;
import me.ferreira.graveto.moneytracker.analytics.service.command.CashFlowCommand;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CashFlowResult;
import me.ferreira.graveto.moneytracker.utils.common.AuthUtils;
import me.ferreira.graveto.moneytracker.utils.common.TestSecurityConfig;
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
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(
    controllers = AnalyticsController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class FetchCashFlowReportControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private AnalyticService service;

  @Test
  void shouldReturnCashFlowReportAndMapToResponseDto() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final int targetYear = 2026;

    final BigDecimal yearlyIncome = BigDecimal.valueOf(5000);
    final BigDecimal yearlyExpense = BigDecimal.valueOf(3000);
    final BigDecimal yearlyNetFlow = BigDecimal.valueOf(2000);

    final CashFlowResult.MonthlyCashFlow januaryFlow = new CashFlowResult.MonthlyCashFlow(
        1, BigDecimal.valueOf(5000), BigDecimal.valueOf(3000), BigDecimal.valueOf(2000)
    );
    final CashFlowResult.MonthlyCashFlow februaryFlow = new CashFlowResult.MonthlyCashFlow(
        2, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
    );

    final CashFlowResult mockResult = new CashFlowResult(
        targetYear,
        yearlyIncome,
        yearlyExpense,
        yearlyNetFlow,
        List.of(januaryFlow, februaryFlow)
    );

    final ArgumentCaptor<CashFlowCommand> commandCaptor = ArgumentCaptor.forClass(CashFlowCommand.class);
    when(service.generateCashFlowReport(commandCaptor.capture())).thenReturn(mockResult);

    // Act
    final MvcTestResult testResult = mvc.get()
        .uri("/analytics/{accountSid}/cash-flow", accountSid)
        .queryParam("year", String.valueOf(targetYear))
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.OK);

    final CashFlowCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.userSid()).isEqualTo(userSid);
    assertThat(capturedCommand.accountSid()).isEqualTo(accountSid);
    assertThat(capturedCommand.year()).isEqualTo(targetYear);

    assertThat(testResult).bodyJson()
        .extractingPath("$.year").asNumber().isEqualTo(targetYear);
    assertThat(testResult).bodyJson()
        .extractingPath("$.yearlyIncome").asNumber().isEqualTo(yearlyIncome.intValue());
    assertThat(testResult).bodyJson()
        .extractingPath("$.yearlyExpense").asNumber().isEqualTo(yearlyExpense.intValue());
    assertThat(testResult).bodyJson()
        .extractingPath("$.yearlyNetFlow").asNumber().isEqualTo(yearlyNetFlow.intValue());

    assertThat(testResult).bodyJson()
        .extractingPath("$.monthlyCashFlow[0].month").asNumber().isEqualTo(1);
    assertThat(testResult).bodyJson()
        .extractingPath("$.monthlyCashFlow[0].income").asNumber().isEqualTo(5000);
    assertThat(testResult).bodyJson()
        .extractingPath("$.monthlyCashFlow[1].month").asNumber().isEqualTo(2);
    assertThat(testResult).bodyJson()
        .extractingPath("$.monthlyCashFlow[1].income").asNumber().isEqualTo(0);
  }

  @Test
  void shouldDefaultToCurrentYearIfNoYearIsProvidedInQuery() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final int expectedDefaultYear = Year.now().getValue();

    final CashFlowResult mockResult = new CashFlowResult(
        expectedDefaultYear, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of()
    );

    final ArgumentCaptor<CashFlowCommand> commandCaptor = ArgumentCaptor.forClass(CashFlowCommand.class);
    when(service.generateCashFlowReport(commandCaptor.capture())).thenReturn(mockResult);

    // Act
    mvc.get()
        .uri("/analytics/{accountSid}/cash-flow", accountSid)
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    final CashFlowCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.year()).isEqualTo(expectedDefaultYear);
  }

  @Test
  void shouldReturnBadRequestForInvalidAccountSid() {
    // Act
    final MvcTestResult testResult = mvc.get()
        .uri("/analytics/{accountSid}/cash-flow", "invalid-uuid-string")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.BAD_REQUEST);
  }

}
