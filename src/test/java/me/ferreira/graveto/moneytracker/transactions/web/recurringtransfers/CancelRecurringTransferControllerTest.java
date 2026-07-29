package me.ferreira.graveto.moneytracker.transactions.web.recurringtransfers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.service.RecurringTransferService;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.CancelRecurringTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.web.RecurringTransferController;
import org.junit.jupiter.api.Test;
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

@WebMvcTest(
    controllers = RecurringTransferController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class CancelRecurringTransferControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private RecurringTransferService service;

  @Test
  void shouldReturnBadRequestForInvalidSidPathVariable() {
    // Act
    final MvcTestResult result = mvc.delete()
        .uri("/recurring-transfers/not-a-uuid")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldCancelRecurringTransferSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();

    final RecurringTransfer mockRt = new RecurringTransfer();
    mockRt.setSid(rtSid);
    mockRt.setSourceAccount(new Account());
    mockRt.setDestinationAccount(new Account());
    mockRt.setUserSid(userSid);
    mockRt.setDescription("Home Insurance");
    mockRt.setAmount(new BigDecimal("50.00"));
    mockRt.setCurrency(Currency.EUR);
    mockRt.setFrequency(Frequency.MONTHLY);
    mockRt.setNextExecutionDate(LocalDate.of(2026, 8, 15));
    mockRt.setStatus(RecurringOperationStatus.CANCELED);
    mockRt.setEndDate(LocalDate.of(2030, 2, 4));

    final ArgumentCaptor<CancelRecurringTransferCommand> commandCaptor =
        ArgumentCaptor.forClass(CancelRecurringTransferCommand.class);
    when(service.cancelRecurringTransfer(commandCaptor.capture())).thenReturn(mockRt);

    // Act
    final MvcTestResult result = mvc.delete()
        .uri("/recurring-transfers/" + rtSid)
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);

    final CancelRecurringTransferCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.sid()).isEqualTo(rtSid);

    assertThat(result).bodyJson().extractingPath("$.sid").asString().isEqualTo(rtSid.toString());
    assertThat(result).bodyJson().extractingPath("$.status").asString().isEqualTo("CANCELED");
  }

}
