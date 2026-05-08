package me.ferreira.graveto.moneytracker.transactions.web.transfer;

import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.FetchTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.TransferService;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import me.ferreira.graveto.moneytracker.transactions.web.TransferController;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@WebMvcTest(
    controllers = TransferController.class,
    excludeFilters = @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "me.ferreira.graveto.identity.*"
))
public class FetchTransferControllerTest {

    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TransferService service;

    @Test
    void shouldReturnTransferAndMapToResponseDTO() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final UUID correlationId = UUID.randomUUID();
        final BigDecimal amount = BigDecimal.valueOf(150);

        final UUID sourceAccountSid = UUID.randomUUID();
        final Account sourceAccount = new Account();
        sourceAccount.setSid(sourceAccountSid);

        final UUID destAccountSid = UUID.randomUUID();
        final Account destAccount = new Account();
        destAccount.setSid(destAccountSid);

        final Transaction mockExpense = new Transaction();
        mockExpense.setAccount(sourceAccount);
        mockExpense.setAmount(amount);
        mockExpense.setCorrelationId(correlationId);
        mockExpense.setStatus(TransactionStatus.ACTIVE);

        final Transaction mockIncome = new Transaction();
        mockIncome.setAccount(destAccount);
        mockIncome.setAmount(amount);
        mockIncome.setCorrelationId(correlationId);
        mockIncome.setStatus(TransactionStatus.ACTIVE);

        final TransferResult transferResult = new TransferResult(mockExpense, mockIncome);

        final ArgumentCaptor<FetchTransferCommand> commandCaptor = ArgumentCaptor.forClass(FetchTransferCommand.class);
        when(service.fetchTransfer(commandCaptor.capture())).thenReturn(transferResult);

        // Act
        final MvcTestResult testResult = mvc.get()
                .uri("/transfers/{correlationId}", correlationId)
                .header("X-User-Sid", userSid)
                .exchange();

        // Assert
        assertThat(testResult).hasStatus(HttpStatus.OK);

        final FetchTransferCommand capturedCommand = commandCaptor.getValue();
        assertThat(capturedCommand.userSid()).isEqualTo(userSid);
        assertThat(capturedCommand.correlationId()).isEqualTo(correlationId);

        assertThat(testResult).bodyJson()
                .extractingPath("$.sourceAccountSid").asString().isEqualTo(sourceAccountSid.toString());
        assertThat(testResult).bodyJson()
                .extractingPath("$.destinationAccountSid").asString().isEqualTo(destAccountSid.toString());
        assertThat(testResult).bodyJson()
                .extractingPath("$.amount").asNumber().isEqualTo(amount.intValue());
        assertThat(testResult).bodyJson()
                .extractingPath("$.correlationId").asString().isEqualTo(correlationId.toString());
        assertThat(testResult).bodyJson()
                .extractingPath("$.transferStatus").asString().isEqualTo(TransactionStatus.ACTIVE.name());

        assertThat(testResult).bodyJson().hasNoNullFieldsOrProperties();
    }

    @Test
    void shouldReturnBadRequestForInvalidCorrelationIdOnTransferFetch() {
        // Act
        final MvcTestResult testResult = mvc.get()
                .uri("/transfers/{correlationId}", "invalid_uuid_string")
                .header("X-User-Sid", UUID.randomUUID())
                .exchange();

        // Assert
        assertThat(testResult).hasStatus(HttpStatus.BAD_REQUEST);
    }

}
