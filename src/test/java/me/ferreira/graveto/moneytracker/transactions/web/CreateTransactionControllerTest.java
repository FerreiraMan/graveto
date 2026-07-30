package me.ferreira.graveto.moneytracker.transactions.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Stream;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.CreateTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.CreateTransactionRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.helper.TransactionDtoAssertions;
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
    controllers = TransactionController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class CreateTransactionControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private TransactionService service;

  private static Stream<Arguments> invalidTransactionCreationRequests() {
    return Stream.of(
        Arguments.of(
            new CreateTransactionRequestDto(null, UUID.randomUUID(), BigDecimal.TEN, "", TransactionType.INCOME,
                LocalDateTime.now()), "accountSid"),
        Arguments.of(
            new CreateTransactionRequestDto(UUID.randomUUID(), null, BigDecimal.TEN, "", TransactionType.INCOME,
                LocalDateTime.now()), "categorySid"),
        Arguments.of(
            new CreateTransactionRequestDto(UUID.randomUUID(), UUID.randomUUID(), null, "", TransactionType.INCOME,
                LocalDateTime.now()), "amount"),
        Arguments.of(new CreateTransactionRequestDto(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO, "",
            TransactionType.INCOME, LocalDateTime.now()), "amount"),
        Arguments.of(new CreateTransactionRequestDto(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN.negate(), "",
            TransactionType.INCOME, LocalDateTime.now()), "amount"),
        Arguments.of(new CreateTransactionRequestDto(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "", null,
            LocalDateTime.now()), "transactionType"),
        Arguments.of(new CreateTransactionRequestDto(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "",
            TransactionType.INCOME, LocalDateTime.now().plusDays(1)), "occurredAt")
    );
  }

  @Test
  void shouldCreateNewTransaction() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID categorySid = UUID.randomUUID();
    final LocalDateTime occurredAt = LocalDateTime.now();

    final CreateTransactionRequestDto request = new CreateTransactionRequestDto(
        accountSid,
        categorySid,
        BigDecimal.TEN,
        "Lunch",
        TransactionType.EXPENSE,
        occurredAt
    );

    final Account mockAccount = new Account();
    mockAccount.setSid(UUID.randomUUID());
    mockAccount.setInstitution("BCP");
    mockAccount.setBaseCurrency(Currency.EUR);
    final Category mockCategory = new Category();
    mockCategory.setSid(UUID.randomUUID());
    mockCategory.setDisplayName("Gas");
    final Transaction mockTransaction =
        Transaction.create(mockAccount, BigDecimal.ONE, null, mockCategory, TransactionType.EXPENSE,
            LocalDateTime.of(2020, 2, 3, 2, 10));

    final ArgumentCaptor<CreateTransactionCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateTransactionCommand.class);
    when(service.createTransaction(commandCaptor.capture())).thenReturn(mockTransaction);

    // Act
    final MvcTestResult testResult = mvc.post()
        .uri("/transactions")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.CREATED);
    assertThat(testResult).hasHeader("Location", "http://localhost/transactions/" + mockTransaction.getSid());

    final CreateTransactionCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.userSid()).isEqualTo(userSid);
    assertThat(capturedCommand.accountSid()).isEqualTo(accountSid);
    assertThat(capturedCommand.categorySid()).isEqualTo(categorySid);
    assertThat(capturedCommand.amount()).isEqualByComparingTo(BigDecimal.TEN);
    assertThat(capturedCommand.description()).isEqualTo("Lunch");
    assertThat(capturedCommand.transactionType()).isEqualTo(TransactionType.EXPENSE);
    assertThat(capturedCommand.occurredAt()).isEqualTo(occurredAt);

    TransactionDtoAssertions.assertSingleResponse(testResult, mockTransaction);
  }

  @Test
  void shouldDefaultOccurredAtIfNoValueIsGivenOnTransactionCreation() {
    // Arrange
    final UUID transactionSid = UUID.randomUUID();
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID categorySid = UUID.randomUUID();
    final BigDecimal amount = BigDecimal.TEN;
    final String description = "Lunch";

    final CreateTransactionRequestDto request = new CreateTransactionRequestDto(
        accountSid,
        categorySid,
        amount,
        description,
        TransactionType.EXPENSE,
        null
    );

    final Transaction mockTransaction = new Transaction();
    mockTransaction.setSid(transactionSid);
    mockTransaction.setStatus(TransactionStatus.ACTIVE);

    final ArgumentCaptor<CreateTransactionCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateTransactionCommand.class);
    when(service.createTransaction(commandCaptor.capture())).thenReturn(mockTransaction);

    // Act
    final MvcTestResult testResult = mvc.post()
        .uri("/transactions")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    final CreateTransactionCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.occurredAt()).isNotNull();
    assertThat(capturedCommand.occurredAt().truncatedTo(ChronoUnit.MINUTES)).isEqualTo(
        LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
  }

  @ParameterizedTest
  @MethodSource("invalidTransactionCreationRequests")
  void shouldReturnBadRequestForInvalidPayloadsOnTransactionCreation(
      final CreateTransactionRequestDto request,
      final String expectedErrorField) {

    final MvcTestResult testResult = mvc.post()
        .uri("/transactions")
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    assertThat(testResult)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .hasPath("$.invalid_params." + expectedErrorField);
  }

}
