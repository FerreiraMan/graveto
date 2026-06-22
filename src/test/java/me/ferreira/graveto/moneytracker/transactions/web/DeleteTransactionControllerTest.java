package me.ferreira.graveto.moneytracker.transactions.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.UUID;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.DeleteTransactionCommand;
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
    controllers = TransactionController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class DeleteTransactionControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private TransactionService service;

  @Test
  void shouldReturnDeletedTransactionAndMapToResponseDto() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID transactionSid = UUID.randomUUID();

    final Transaction mockTransaction = new Transaction();
    mockTransaction.setSid(transactionSid);
    mockTransaction.setStatus(TransactionStatus.ACTIVE);

    final ArgumentCaptor<DeleteTransactionCommand> commandCaptor =
        ArgumentCaptor.forClass(DeleteTransactionCommand.class);
    when(service.deleteTransaction(commandCaptor.capture())).thenReturn(mockTransaction);

    // Act
    final MvcTestResult testResult = mvc.delete()
        .uri("/transactions/{sid}", transactionSid)
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.OK);

    final DeleteTransactionCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.userSid()).isEqualTo(userSid);
    assertThat(capturedCommand.transactionSid()).isEqualTo(transactionSid);

    assertThat(testResult).bodyJson()
        .extractingPath("$.sid").asString().isEqualTo(transactionSid.toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$.status").asString().isEqualTo(TransactionStatus.ACTIVE.name());

    assertThat(testResult).bodyJson().hasNoNullFieldsOrProperties();
    assertThat(testResult).bodyJson().doesNotHavePath("$.amount");
    assertThat(testResult).bodyJson().doesNotHavePath("$.categoryName");
    assertThat(testResult).bodyJson().doesNotHavePath("$.description");
    assertThat(testResult).bodyJson().doesNotHavePath("$.type");
    assertThat(testResult).bodyJson().doesNotHavePath("$.occurredAt");
  }

  @Test
  void shouldReturnBadRequestForInvalidRequestOnTransactionDelete() {

    final MvcTestResult testResult = mvc.delete()
        .uri("/transactions/{transactionSid}", "invalid_sid")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    assertThat(testResult)
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

}
