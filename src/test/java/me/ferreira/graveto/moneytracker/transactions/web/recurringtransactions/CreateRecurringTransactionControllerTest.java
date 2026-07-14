package me.ferreira.graveto.moneytracker.transactions.web.recurringtransactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;
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
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.CreateRecurringTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.web.RecurringTransactionController;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransaction.CreateRecurringTransactionDto;
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
    controllers = RecurringTransactionController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class CreateRecurringTransactionControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private RecurringTransactionService service;

  private static Stream<Arguments> invalidRequests() {
    return Stream.of(
        Arguments.of(new CreateRecurringTransactionDto(
            null, UUID.randomUUID(), "desc", BigDecimal.TEN, TransactionType.EXPENSE,
            Frequency.MONTHLY, 15, null, true, null, null), "accountSid"),
        Arguments.of(new CreateRecurringTransactionDto(
            UUID.randomUUID(), null, "desc", BigDecimal.TEN, TransactionType.EXPENSE,
            Frequency.MONTHLY, 15, null, true, null, null), "categorySid"),
        Arguments.of(new CreateRecurringTransactionDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", null, TransactionType.EXPENSE,
            Frequency.MONTHLY, 15, null, true, null, null), "amount"),
        Arguments.of(new CreateRecurringTransactionDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.ZERO, TransactionType.EXPENSE,
            Frequency.MONTHLY, 15, null, true, null, null), "amount"),
        Arguments.of(new CreateRecurringTransactionDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.TEN.negate(), TransactionType.EXPENSE,
            Frequency.MONTHLY, 15, null, true, null, null), "amount"),
        Arguments.of(new CreateRecurringTransactionDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.TEN, null,
            Frequency.MONTHLY, 15, null, true, null, null), "transactionType"),
        Arguments.of(new CreateRecurringTransactionDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.TEN, TransactionType.EXPENSE,
            null, 15, null, true, null, null), "frequency"),
        Arguments.of(new CreateRecurringTransactionDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.TEN, TransactionType.EXPENSE,
            Frequency.MONTHLY, null, null, null, null, null), "adjustToBusinessDay"),
        Arguments.of(new CreateRecurringTransactionDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.TEN, TransactionType.EXPENSE,
            Frequency.MONTHLY, 0, null, true, null, null), "dayOfMonth"),
        Arguments.of(new CreateRecurringTransactionDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.TEN, TransactionType.EXPENSE,
            Frequency.MONTHLY, 32, null, true, null, null), "dayOfMonth"),
        Arguments.of(new CreateRecurringTransactionDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.TEN, TransactionType.EXPENSE,
            Frequency.WEEKLY, null, 0, true, null, null), "dayOfWeek"),
        Arguments.of(new CreateRecurringTransactionDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.TEN, TransactionType.EXPENSE,
            Frequency.WEEKLY, null, 8, true, null, null), "dayOfWeek")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidRequests")
  void shouldReturnBadRequestForInvalidPayloads(
      final CreateRecurringTransactionDto request,
      final String expectedErrorField) {

    final MvcTestResult result = mvc.post()
        .uri("/recurring-transactions")
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
  void shouldCreateRecurringTransactionSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID categorySid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final LocalDate nextExecution = LocalDate.of(2026, 8, 15);

    final CreateRecurringTransactionDto request = new CreateRecurringTransactionDto(
        accountSid, categorySid, "Home Insurance", new BigDecimal("50.00"), TransactionType.EXPENSE,
        Frequency.MONTHLY, 15, null, true, null, null);

    final Account mockAccount = new Account();
    mockAccount.setSid(accountSid);
    mockAccount.setInstitution("Santander");
    mockAccount.setBaseCurrency(Currency.EUR);

    final Category mockCategory = new Category();
    mockCategory.setSid(categorySid);
    mockCategory.setDisplayName("Insurance");

    final RecurringTransaction mockRt = new RecurringTransaction();
    mockRt.setSid(rtSid);
    mockRt.setAccount(mockAccount);
    mockRt.setCategory(mockCategory);
    mockRt.setUserSid(userSid);
    mockRt.setDescription("Home Insurance");
    mockRt.setAmount(new BigDecimal("50.00"));
    mockRt.setCurrency(Currency.EUR);
    mockRt.setType(TransactionType.EXPENSE);
    mockRt.setFrequency(Frequency.MONTHLY);
    mockRt.setNextExecutionDate(nextExecution);
    mockRt.setStatus(RecurringOperationStatus.ACTIVE);
    mockRt.setEndDate(null);

    final ArgumentCaptor<CreateRecurringTransactionCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateRecurringTransactionCommand.class);
    when(service.createRecurringTransaction(commandCaptor.capture())).thenReturn(mockRt);

    // Act
    final MvcTestResult result = mvc.post()
        .uri("/recurring-transactions")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.CREATED);
    assertThat(result).hasHeader("Location", "http://localhost/recurring-transactions/" + rtSid);

    final CreateRecurringTransactionCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.accountSid()).isEqualTo(accountSid);
    assertThat(captured.categorySid()).isEqualTo(categorySid);
    assertThat(captured.description()).isEqualTo("Home Insurance");
    assertThat(captured.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
    assertThat(captured.transactionType()).isEqualTo(TransactionType.EXPENSE);
    assertThat(captured.frequency()).isEqualTo(Frequency.MONTHLY);
    assertThat(captured.dayOfMonth()).isEqualTo(15);
    assertThat(captured.adjustToBusinessDay()).isTrue();

    assertThat(result).bodyJson().extractingPath("$.sid").asString().isEqualTo(rtSid.toString());
    assertThat(result).bodyJson().extractingPath("$.account.name").asString().isEqualTo("Santander");
    assertThat(result).bodyJson().extractingPath("$.category.name").asString().isEqualTo("Insurance");
    assertThat(result).bodyJson().extractingPath("$.frequency").asString().isEqualTo("MONTHLY");
    assertThat(result).bodyJson().extractingPath("$.status").asString().isEqualTo("ACTIVE");
    assertThat(result).bodyJson().extractingPath("$.nextExecutionDate").asString().isEqualTo("2026-08-15");
  }

  @Test
  void shouldTrimDescriptionBeforePassingToService() {
    // Arrange
    final CreateRecurringTransactionDto request = new CreateRecurringTransactionDto(
        UUID.randomUUID(), UUID.randomUUID(), "  Home Insurance  ", new BigDecimal("50.00"),
        TransactionType.EXPENSE, Frequency.MONTHLY, 15, null, true, null, null);

    final RecurringTransaction mockRt = buildMinimalMockRt();

    final ArgumentCaptor<CreateRecurringTransactionCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateRecurringTransactionCommand.class);
    when(service.createRecurringTransaction(commandCaptor.capture())).thenReturn(mockRt);

    // Act
    mvc.post()
        .uri("/recurring-transactions")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(commandCaptor.getValue().description()).isEqualTo("Home Insurance");
  }

  private RecurringTransaction buildMinimalMockRt() {
    final Account account = new Account();
    account.setSid(UUID.randomUUID());
    account.setInstitution("Santander");

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
    rt.setNextExecutionDate(LocalDate.now().plusMonths(1));
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    rt.setEndDate(null);
    return rt;
  }

}
