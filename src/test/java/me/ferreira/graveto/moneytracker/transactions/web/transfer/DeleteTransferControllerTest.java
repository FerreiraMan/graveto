package me.ferreira.graveto.moneytracker.transactions.web.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.util.UUID;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.DeleteTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.TransferService;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import me.ferreira.graveto.moneytracker.transactions.web.TransferController;
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
    controllers = TransferController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class DeleteTransferControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private TransferService service;

  @Test
  void shouldReturnDeletedTransferAndMapToResponseDto() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final BigDecimal amount = BigDecimal.TEN;
    final UUID transactionOutSid = UUID.randomUUID();
    final UUID transactionInSid = UUID.randomUUID();
    final UUID correlationId = UUID.randomUUID();

    final Account accountOut = new Account();
    final UUID accountOutSid = UUID.randomUUID();
    accountOut.setSid(accountOutSid);
    final Transaction mockTransactionOut = new Transaction();
    mockTransactionOut.setSid(transactionOutSid);
    mockTransactionOut.setStatus(TransactionStatus.ACTIVE);
    mockTransactionOut.setAmount(amount);
    mockTransactionOut.setCorrelationId(correlationId);
    mockTransactionOut.setAccount(accountOut);

    final Account accountIn = new Account();
    final UUID accountInSid = UUID.randomUUID();
    accountIn.setSid(accountInSid);
    final Transaction mockTransactionIn = new Transaction();
    mockTransactionIn.setSid(transactionInSid);
    mockTransactionIn.setStatus(TransactionStatus.ACTIVE);
    mockTransactionIn.setAmount(amount);
    mockTransactionIn.setCorrelationId(correlationId);
    mockTransactionIn.setAccount(accountIn);

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

    assertThat(testResult).bodyJson()
        .extractingPath("$.sourceAccountSid").asString().isEqualTo(accountOutSid.toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$.destinationAccountSid").asString().isEqualTo(accountInSid.toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$.amount").asNumber().isEqualTo(mockTransactionOut.getAmount().intValue());
    assertThat(testResult).bodyJson()
        .extractingPath("$.correlationId").asString().isEqualTo(correlationId.toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$.transferStatus").asString().isEqualTo(TransactionStatus.DELETED.name());

    assertThat(testResult).bodyJson().hasNoNullFieldsOrProperties();
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
