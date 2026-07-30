package me.ferreira.graveto.moneytracker.transactions.web.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.DeleteTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.TransferService;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import me.ferreira.graveto.moneytracker.transactions.web.TransferController;
import me.ferreira.graveto.moneytracker.transactions.web.helper.TransferDtoAssertions;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
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
    controllers = TransferController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class DeleteTransferControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private TransferService service;

  @Test
  void shouldReturnDeletedTransferAndMapToResponseDto() {
    // Arrange
    final Account sourceAccount = AccountUtils.createAccount(BigDecimal.TEN);
    final Account destinationAccount = AccountUtils.createAccount(BigDecimal.TEN);
    final Category mockCategory = new Category();
    mockCategory.setSid(UUID.randomUUID());
    mockCategory.setDisplayName("Gas");

    final UUID userSid = UUID.randomUUID();
    final UUID correlationId = UUID.randomUUID();
    final LocalDateTime occurredAt = LocalDateTime.of(2020, 2, 3, 2, 10);

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

    final ArgumentCaptor<DeleteTransferCommand> commandCaptor = ArgumentCaptor.forClass(DeleteTransferCommand.class);
    when(service.deleteTransfer(commandCaptor.capture())).thenReturn(transferResult);

    // Act
    final MvcTestResult testResult = mvc.delete()
        .uri("/transfers/{correlationId}", correlationId)
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.OK);

    final DeleteTransferCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.userSid()).isEqualTo(userSid);
    assertThat(capturedCommand.correlationId()).isEqualTo(correlationId);

    TransferDtoAssertions.assertResponse(testResult, transferResult);
  }

  @Test
  void shouldReturnBadRequestForInvalidRequestOnTransferDelete() {

    final MvcTestResult testResult = mvc.delete()
        .uri("/transfers/{correlationId}", "invalid_sid")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    assertThat(testResult)
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

}
