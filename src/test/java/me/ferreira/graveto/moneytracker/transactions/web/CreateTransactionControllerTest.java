package me.ferreira.graveto.moneytracker.transactions.web;

import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.CreateTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.CreateTransactionRequestDTO;
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
    controllers = TransactionController.class,
    excludeFilters = @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "me.ferreira.graveto.identity.*"
))
public class CreateTransactionControllerTest {

    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TransactionService service;

    @Test
    void shouldCreateNewTransaction() {
        // Arrange
        final UUID transactionSid = UUID.randomUUID();
        final UUID userSid = UUID.randomUUID();
        final UUID accountSid = UUID.randomUUID();
        final UUID categorySid = UUID.randomUUID();
        final BigDecimal amount = BigDecimal.TEN;
        final String description = "Lunch";
        final LocalDateTime occurredAt = LocalDateTime.now();

        final CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                accountSid,
                categorySid,
                amount,
                description,
                TransactionType.EXPENSE,
                occurredAt
        );

        final Category mockCategory = new Category();
        mockCategory.setDisplayName("Gas");

        final Transaction mockTransaction = new Transaction();
        mockTransaction.setSid(transactionSid);
        mockTransaction.setStatus(TransactionStatus.ACTIVE);
        mockTransaction.setType(TransactionType.EXPENSE);
        mockTransaction.setCategory(mockCategory);

        final ArgumentCaptor<CreateTransactionCommand> commandCaptor = ArgumentCaptor.forClass(CreateTransactionCommand.class);
        when(service.createTransaction(commandCaptor.capture())).thenReturn(mockTransaction);

        // Act
        final MvcTestResult testResult = mvc.post()
                .uri("/transactions")
                .header("X-User-Sid", userSid)
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange();

        // Assert
        assertThat(testResult).hasStatus(HttpStatus.CREATED);
        assertThat(testResult).hasHeader("Location", "http://localhost/transactions/" + transactionSid);

        final CreateTransactionCommand capturedCommand = commandCaptor.getValue();
        assertThat(capturedCommand.userSid()).isEqualTo(userSid);
        assertThat(capturedCommand.accountSid()).isEqualTo(accountSid);
        assertThat(capturedCommand.categorySid()).isEqualTo(categorySid);
        assertThat(capturedCommand.amount()).isEqualByComparingTo(amount);
        assertThat(capturedCommand.description()).isEqualTo(description);
        assertThat(capturedCommand.transactionType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(capturedCommand.occurredAt()).isEqualTo(occurredAt);


        assertThat(testResult).bodyJson()
                .extractingPath("$.sid").asString().isEqualTo(transactionSid.toString());
        assertThat(testResult).bodyJson()
                .extractingPath("$.status").asString().isEqualTo(TransactionStatus.ACTIVE.name());
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

        final CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
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

        final ArgumentCaptor<CreateTransactionCommand> commandCaptor = ArgumentCaptor.forClass(CreateTransactionCommand.class);
        when(service.createTransaction(commandCaptor.capture())).thenReturn(mockTransaction);

        // Act
        final MvcTestResult testResult = mvc.post()
                .uri("/transactions")
                .header("X-User-Sid", userSid)
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange();

        // Assert
        final CreateTransactionCommand capturedCommand = commandCaptor.getValue();
        assertThat(capturedCommand.occurredAt()).isNotNull();
        assertThat(capturedCommand.occurredAt().truncatedTo(ChronoUnit.MINUTES)).isEqualTo(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
    }

    @ParameterizedTest
    @MethodSource("invalidTransactionCreationRequests")
    void shouldReturnBadRequestForInvalidPayloadsOnTransactionCreation(
            final CreateTransactionRequestDTO request,
            final String expectedErrorField) {

        final MvcTestResult testResult = mvc.post()
                .uri("/transactions")
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

    private static Stream<Arguments> invalidTransactionCreationRequests() {
        return Stream.of(
                Arguments.of(new CreateTransactionRequestDTO(null, UUID.randomUUID(), BigDecimal.TEN, "", TransactionType.INCOME, LocalDateTime.now()), "accountSid"),
                Arguments.of(new CreateTransactionRequestDTO(UUID.randomUUID(), null, BigDecimal.TEN, "", TransactionType.INCOME, LocalDateTime.now()), "categorySid"),
                Arguments.of(new CreateTransactionRequestDTO(UUID.randomUUID(), UUID.randomUUID(), null, "", TransactionType.INCOME, LocalDateTime.now()), "amount"),
                Arguments.of(new CreateTransactionRequestDTO(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO, "", TransactionType.INCOME, LocalDateTime.now()), "amount"),
                Arguments.of(new CreateTransactionRequestDTO(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN.negate(), "", TransactionType.INCOME, LocalDateTime.now()), "amount"),
                Arguments.of(new CreateTransactionRequestDTO(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "", null, LocalDateTime.now()), "transactionType"),
                Arguments.of(new CreateTransactionRequestDTO(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "", TransactionType.INCOME, LocalDateTime.now().plusDays(1)), "occurredAt")
        );
    }

}
