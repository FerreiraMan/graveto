package me.ferreira.graveto.moneytracker.transactions.web.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Stream;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.CreateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.TransferService;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import me.ferreira.graveto.moneytracker.transactions.web.TransferController;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transfer.CreateTransferRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.helper.TransferDtoAssertions;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
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
    controllers = TransferController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class CreateTransferControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private TransferService service;

  private static Stream<Arguments> invalidTransferCreationRequests() {
    return Stream.of(
        Arguments.of(new CreateTransferRequestDto(null, UUID.randomUUID(), BigDecimal.TEN, "", LocalDateTime.now()),
            "sourceAccountSid"),
        Arguments.of(new CreateTransferRequestDto(UUID.randomUUID(), null, BigDecimal.TEN, "", LocalDateTime.now()),
            "destinationAccountSid"),
        Arguments.of(new CreateTransferRequestDto(UUID.randomUUID(), UUID.randomUUID(), null, "", LocalDateTime.now()),
            "amount"),
        Arguments.of(new CreateTransferRequestDto(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO, "",
            LocalDateTime.now()), "amount"),
        Arguments.of(new CreateTransferRequestDto(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN.negate(), "",
            LocalDateTime.now()), "amount"),
        Arguments.of(new CreateTransferRequestDto(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "",
            LocalDateTime.now().plusDays(1)), "occurredAt")
    );
  }

  @Test
  void shouldCreateNewTransfer() {
    // Arrange
    final Account sourceAccount = AccountUtils.createAccount(BigDecimal.TEN);
    final Account destinationAccount = AccountUtils.createAccount(BigDecimal.TEN);
    final Category mockCategory = new Category();
    mockCategory.setSid(UUID.randomUUID());
    mockCategory.setDisplayName("Gas");

    final UUID userSid = UUID.randomUUID();
    final UUID correlationId = UUID.randomUUID();
    final LocalDateTime occurredAt = LocalDateTime.of(2020, 2, 3, 2, 10);

    final CreateTransferRequestDto request = new CreateTransferRequestDto(
        sourceAccount.getSid(),
        destinationAccount.getSid(),
        BigDecimal.TEN,
        "Lunch",
        occurredAt
    );

    final Transaction mockTransactionOut = Transaction.createTransferTransaction(
        sourceAccount,
        BigDecimal.ONE,
        "Lunch",
        correlationId,
        mockCategory,
        TransactionType.TRANSFER_OUT,
        occurredAt
    );

    final Transaction mockTransactionIn = Transaction.createTransferTransaction(
        destinationAccount,
        BigDecimal.ONE,
        "Lunch",
        correlationId,
        mockCategory,
        TransactionType.TRANSFER_IN,
        occurredAt
    );

    final TransferResult transferResult = new TransferResult(mockTransactionOut, mockTransactionIn);

    final ArgumentCaptor<CreateTransferCommand> commandCaptor = ArgumentCaptor.forClass(CreateTransferCommand.class);
    when(service.createTransfer(commandCaptor.capture())).thenReturn(transferResult);

    // Act
    final MvcTestResult testResult = mvc.post()
        .uri("/transfers")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.CREATED);
    assertThat(testResult).hasHeader("Location", "http://localhost/transfers/" + correlationId);

    final CreateTransferCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.userSid()).isEqualTo(userSid);
    assertThat(capturedCommand.sourceAccountSid()).isEqualTo(sourceAccount.getSid());
    assertThat(capturedCommand.destinationAccountSid()).isEqualTo(destinationAccount.getSid());
    assertThat(capturedCommand.amount()).isEqualByComparingTo(BigDecimal.TEN);
    assertThat(capturedCommand.description()).isEqualTo("Lunch");
    assertThat(capturedCommand.occurredAt()).isEqualTo(occurredAt);

    TransferDtoAssertions.assertResponse(testResult, transferResult);
  }

  @Test
  void shouldDefaultOccurredAtIfNoValueIsGivenOnTransferCreation() {
    // Arrange
    final UUID transactionOutSid = UUID.randomUUID();
    final UUID transactionInSid = UUID.randomUUID();
    final UUID userSid = UUID.randomUUID();
    final UUID sourceAccountSid = UUID.randomUUID();
    final UUID destinationAccountSid = UUID.randomUUID();
    final BigDecimal amount = BigDecimal.TEN;
    final String description = "Lunch";

    final CreateTransferRequestDto request = new CreateTransferRequestDto(
        sourceAccountSid,
        destinationAccountSid,
        amount,
        description,
        null
    );

    final Transaction mockTransactionOut = new Transaction();
    final Transaction mockTransactionIn = new Transaction();
    mockTransactionOut.setSid(transactionOutSid);
    mockTransactionIn.setSid(transactionInSid);
    final TransferResult transferResult = new TransferResult(mockTransactionOut, mockTransactionIn);

    final ArgumentCaptor<CreateTransferCommand> commandCaptor = ArgumentCaptor.forClass(CreateTransferCommand.class);
    when(service.createTransfer(commandCaptor.capture())).thenReturn(transferResult);

    // Act
    mvc.post()
        .uri("/transfers")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    final CreateTransferCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.occurredAt()).isNotNull();
    assertThat(capturedCommand.occurredAt().truncatedTo(ChronoUnit.MINUTES)).isEqualTo(
        LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
  }

  @ParameterizedTest
  @MethodSource("invalidTransferCreationRequests")
  void shouldReturnBadRequestForInvalidPayloadsOnTransferCreation(
      final CreateTransferRequestDto request,
      final String expectedErrorField) {

    final MvcTestResult testResult = mvc.post()
        .uri("/transfers")
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
