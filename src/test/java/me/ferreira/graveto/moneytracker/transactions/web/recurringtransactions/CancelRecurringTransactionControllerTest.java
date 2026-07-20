package me.ferreira.graveto.moneytracker.transactions.web.recurringtransactions;

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
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.service.RecurringTransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.CancelRecurringTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.web.RecurringTransactionController;
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
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(
    controllers = RecurringTransactionController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class CancelRecurringTransactionControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private RecurringTransactionService service;

  @Test
  void shouldReturnBadRequestForInvalidSidPathVariable() {
    // Act
    final MvcTestResult result = mvc.delete()
        .uri("/recurring-transactions/not-a-uuid")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldCancelRecurringTransactionSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();

    final RecurringTransaction mockRt = new RecurringTransaction();
    mockRt.setSid(rtSid);
    mockRt.setAccount(new Account());
    mockRt.setCategory(new Category());
    mockRt.setUserSid(userSid);
    mockRt.setDescription("Home Insurance");
    mockRt.setAmount(new BigDecimal("50.00"));
    mockRt.setCurrency(Currency.EUR);
    mockRt.setType(TransactionType.EXPENSE);
    mockRt.setFrequency(Frequency.MONTHLY);
    mockRt.setNextExecutionDate(LocalDate.of(2026, 8, 15));
    mockRt.setStatus(RecurringOperationStatus.CANCELED);
    mockRt.setEndDate(LocalDate.of(2030, 2, 4));

    final ArgumentCaptor<CancelRecurringTransactionCommand> commandCaptor =
        ArgumentCaptor.forClass(CancelRecurringTransactionCommand.class);
    when(service.cancelRecurringTransaction(commandCaptor.capture())).thenReturn(mockRt);

    // Act
    final MvcTestResult result = mvc.delete()
        .uri("/recurring-transactions/" + rtSid)
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);

    final CancelRecurringTransactionCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.sid()).isEqualTo(rtSid);

    assertThat(result).bodyJson().extractingPath("$.sid").asString().isEqualTo(rtSid.toString());
    assertThat(result).bodyJson().extractingPath("$.status").asString().isEqualTo("CANCELED");
  }

}
