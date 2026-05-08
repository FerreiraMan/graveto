package me.ferreira.graveto.moneytracker.transactions.web.transfer;

import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.CreateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.TransferService;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import me.ferreira.graveto.moneytracker.transactions.web.TransferController;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transfer.CreateTransferRequestDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@WebMvcTest(
    controllers = TransferController.class,
    excludeFilters = @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "me.ferreira.graveto.identity.*"
))
public class CreateTransferControllerTest {

    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TransferService service;

    @Test
    void shouldCreateNewTransfer() {
        // Arrange
        final Account sourceAccount = AccountUtils.createAccount(BigDecimal.TEN);
        final Account destinationAccount = AccountUtils.createAccount(BigDecimal.TEN);
        final UUID sourceAccountSid = sourceAccount.getSid();
        final UUID destinationAccountSid = destinationAccount.getSid();

        final UUID transactionOutSid = UUID.randomUUID();
        final UUID transactionInSid = UUID.randomUUID();
        final UUID correlationId = UUID.randomUUID();
        final UUID userSid = UUID.randomUUID();
        final BigDecimal amount = BigDecimal.TEN;
        final String description = "Lunch";
        final LocalDateTime occurredAt = LocalDateTime.now();

        final CreateTransferRequestDTO request = new CreateTransferRequestDTO(
                sourceAccountSid,
                destinationAccountSid,
                amount,
                description,
                occurredAt
        );

        final Transaction mockTransactionOut = new Transaction();
        final Transaction mockTransactionIn = new Transaction();
        mockTransactionOut.setSid(transactionOutSid);
        mockTransactionOut.setCorrelationId(correlationId);
        mockTransactionOut.setAmount(amount);
        mockTransactionOut.setAccount(sourceAccount);
        mockTransactionIn.setSid(transactionInSid);
        mockTransactionIn.setCorrelationId(correlationId);
        mockTransactionIn.setAmount(amount);
        mockTransactionIn.setAccount(destinationAccount);
        final TransferResult transferResult = new TransferResult(mockTransactionOut, mockTransactionIn);

        final ArgumentCaptor<CreateTransferCommand> commandCaptor = ArgumentCaptor.forClass(CreateTransferCommand.class);
        when(service.createTransfer(commandCaptor.capture())).thenReturn(transferResult);

        // Act
        final MvcTestResult testResult = mvc.post()
                .uri("/transfers")
                .header("X-User-Sid", userSid)
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange();

        // Assert
        assertThat(testResult).hasStatus(HttpStatus.CREATED);
        assertThat(testResult).hasHeader("Location", "http://localhost/transfers/" + correlationId);

        final CreateTransferCommand capturedCommand = commandCaptor.getValue();
        assertThat(capturedCommand.userSid()).isEqualTo(userSid);
        assertThat(capturedCommand.sourceAccountSid()).isEqualTo(sourceAccountSid);
        assertThat(capturedCommand.destinationAccountSid()).isEqualTo(destinationAccountSid);
        assertThat(capturedCommand.amount()).isEqualByComparingTo(amount);
        assertThat(capturedCommand.description()).isEqualTo(description);
        assertThat(capturedCommand.occurredAt()).isEqualTo(occurredAt);

        assertThat(testResult).bodyJson()
                .extractingPath("$.sourceAccountSid").asString().isEqualTo(sourceAccountSid.toString());
        assertThat(testResult).bodyJson()
                .extractingPath("$.destinationAccountSid").asString().isEqualTo(destinationAccountSid.toString());
        assertThat(testResult).bodyJson()
                .extractingPath("$.amount").asNumber().isEqualTo(amount.intValue());
        assertThat(testResult).bodyJson()
                .extractingPath("$.correlationId").asString().isEqualTo(correlationId.toString());
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

        final CreateTransferRequestDTO request = new CreateTransferRequestDTO(
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
            .header("X-User-Sid", userSid)
            .content(objectMapper.writeValueAsString(request))
            .contentType(MediaType.APPLICATION_JSON)
            .exchange();

        // Assert
        final CreateTransferCommand capturedCommand = commandCaptor.getValue();
        assertThat(capturedCommand.occurredAt()).isNotNull();
        assertThat(capturedCommand.occurredAt().truncatedTo(ChronoUnit.MINUTES)).isEqualTo(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
    }

    @ParameterizedTest
    @MethodSource("invalidTransferCreationRequests")
    void shouldReturnBadRequestForInvalidPayloadsOnTransferCreation(
            final CreateTransferRequestDTO request,
            final String expectedErrorField) {

        final MvcTestResult testResult = mvc.post()
                .uri("/transfers")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Sid", UUID.randomUUID())
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        assertThat(testResult)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPath("$.invalid_params." + expectedErrorField);
    }

    private static Stream<Arguments> invalidTransferCreationRequests() {
        return Stream.of(
                Arguments.of(new CreateTransferRequestDTO(null, UUID.randomUUID(), BigDecimal.TEN, "", LocalDateTime.now()), "sourceAccountSid"),
                Arguments.of(new CreateTransferRequestDTO(UUID.randomUUID(), null, BigDecimal.TEN, "", LocalDateTime.now()), "destinationAccountSid"),
                Arguments.of(new CreateTransferRequestDTO(UUID.randomUUID(), UUID.randomUUID(), null, "", LocalDateTime.now()), "amount"),
                Arguments.of(new CreateTransferRequestDTO(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO, "", LocalDateTime.now()), "amount"),
                Arguments.of(new CreateTransferRequestDTO(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN.negate(), "", LocalDateTime.now()), "amount"),
                Arguments.of(new CreateTransferRequestDTO(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "", LocalDateTime.now().plusDays(1)), "occurredAt")
        );
    }

}
