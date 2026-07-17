package me.ferreira.graveto.moneytracker.transactions.web.recurringtransactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.FindAllRecurringTransactionsCommand;
import me.ferreira.graveto.moneytracker.transactions.web.RecurringTransactionController;
import org.assertj.core.api.InstanceOfAssertFactories;
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
    controllers = RecurringTransactionController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class FindAllRecurringTransactionsControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private RecurringTransactionService service;

  @Test
  void shouldFetchAllRecurringTransactionsSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final RecurringTransaction rt = buildMockRecurringTransaction();

    final ArgumentCaptor<FindAllRecurringTransactionsCommand> commandCaptor =
        ArgumentCaptor.forClass(FindAllRecurringTransactionsCommand.class);
    when(service.fetchAllRecurringTransactions(commandCaptor.capture())).thenReturn(List.of(rt));

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/recurring-transactions")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);

    final FindAllRecurringTransactionsCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.status()).isNull();
    assertThat(captured.accountSid()).isNull();

    assertThat(result).bodyJson().extractingPath("$[0].sid").asString().isEqualTo(rt.getSid().toString());
    assertThat(result).bodyJson().extractingPath("$[0].frequency").asString().isEqualTo("MONTHLY");
    assertThat(result).bodyJson().extractingPath("$[0].status").asString().isEqualTo("ACTIVE");
    assertThat(result).bodyJson().extractingPath("$[0].account.name").asString().isEqualTo("Santander");
  }

  @Test
  void shouldPassStatusFilterToCommand() {
    // Arrange
    final UUID userSid = UUID.randomUUID();

    final ArgumentCaptor<FindAllRecurringTransactionsCommand> commandCaptor =
        ArgumentCaptor.forClass(FindAllRecurringTransactionsCommand.class);
    when(service.fetchAllRecurringTransactions(commandCaptor.capture())).thenReturn(List.of());

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/recurring-transactions")
        .param("status", "ACTIVE")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(commandCaptor.getValue().status()).isEqualTo(RecurringOperationStatus.ACTIVE);
  }

  @Test
  void shouldPassAccountSidFilterToCommand() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();

    final ArgumentCaptor<FindAllRecurringTransactionsCommand> commandCaptor =
        ArgumentCaptor.forClass(FindAllRecurringTransactionsCommand.class);
    when(service.fetchAllRecurringTransactions(commandCaptor.capture())).thenReturn(List.of());

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/recurring-transactions")
        .param("accountSid", accountSid.toString())
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(commandCaptor.getValue().accountSid()).isEqualTo(accountSid);
  }

  @Test
  void shouldReturnEmptyListWhenNoRecurringTransactionsExist() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    when(service.fetchAllRecurringTransactions(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/recurring-transactions")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(result).bodyJson().extractingPath("$").asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty();
  }

  private RecurringTransaction buildMockRecurringTransaction() {
    final Account account = new Account();
    account.setSid(UUID.randomUUID());
    account.setInstitution("Santander");
    account.setBaseCurrency(Currency.EUR);

    final Category category = new Category();
    category.setSid(UUID.randomUUID());
    category.setDisplayName("Insurance");

    final RecurringTransaction rt = new RecurringTransaction();
    rt.setSid(UUID.randomUUID());
    rt.setAccount(account);
    rt.setCategory(category);
    rt.setUserSid(UUID.randomUUID());
    rt.setDescription("Home Insurance");
    rt.setAmount(new BigDecimal("50.00"));
    rt.setCurrency(Currency.EUR);
    rt.setType(TransactionType.EXPENSE);
    rt.setFrequency(Frequency.MONTHLY);
    rt.setDayOfTheMonth(15);
    rt.setAdjustToBusinessDay(true);
    rt.setNextExecutionDate(LocalDate.of(2026, 8, 15));
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    rt.setEndDate(null);
    return rt;
  }

}
