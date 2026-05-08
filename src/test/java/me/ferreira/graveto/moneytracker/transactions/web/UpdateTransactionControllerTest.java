package me.ferreira.graveto.moneytracker.transactions.web;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.UpdateTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.UpdateTransactionRequestDTO;
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
public class UpdateTransactionControllerTest {

    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TransactionService service;

    @Test
    void shouldReturnUpdatedTransactionAndMapToResponseDTO() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final UUID transactionSid = UUID.randomUUID();
        final UUID categorySid = UUID.randomUUID();
        final Transaction mockTransaction = getTransaction(transactionSid);
        final LocalDateTime newOccurredAt = LocalDateTime.now();

        final UpdateTransactionRequestDTO requestDTO = new UpdateTransactionRequestDTO(
                TransactionType.EXPENSE,
                categorySid,
                BigDecimal.TEN,
                "Diesel for car 2",
                newOccurredAt
        );

        final ArgumentCaptor<UpdateTransactionCommand> commandCaptor = ArgumentCaptor.forClass(UpdateTransactionCommand.class);
        when(service.updateTransaction(commandCaptor.capture())).thenReturn(mockTransaction);

        // Act
        final MvcTestResult testResult = mvc.patch()
                .uri("/transactions/{sid}", transactionSid)
                .header("X-User-Sid", userSid)
                .content(objectMapper.writeValueAsString(requestDTO))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange();

        // Assert
        assertThat(testResult).hasStatus(HttpStatus.OK);

        final UpdateTransactionCommand capturedCommand = commandCaptor.getValue();
        assertThat(capturedCommand.userSid()).isEqualTo(userSid);
        assertThat(capturedCommand.transactionSid()).isEqualTo(transactionSid);
        assertThat(capturedCommand.transactionType()).isEqualTo(requestDTO.transactionType());
        assertThat(capturedCommand.categorySid()).isEqualTo(requestDTO.categorySid());
        assertThat(capturedCommand.amount()).isEqualByComparingTo(requestDTO.amount());
        assertThat(capturedCommand.description()).isEqualTo(requestDTO.description());
        assertThat(capturedCommand.occurredAt()).isEqualTo(requestDTO.occurredAt());

        assertThat(testResult).bodyJson()
                .extractingPath("$.sid").asString().isEqualTo(mockTransaction.getSid().toString());
        assertThat(testResult).bodyJson()
                .extractingPath("$.amount").asNumber().isEqualTo(mockTransaction.getAmount().intValue());
        assertThat(testResult).bodyJson()
                .extractingPath("$.categoryName").isEqualTo(mockTransaction.getCategory().getDisplayName());
        assertThat(testResult).bodyJson()
                .extractingPath("$.description").isEqualTo(mockTransaction.getDescription());
        assertThat(testResult).bodyJson()
                .extractingPath("$.type").asString().isEqualTo(mockTransaction.getType().name());

        assertThat(testResult).bodyJson().hasNoNullFieldsOrProperties();
        assertThat(testResult).bodyJson().doesNotHavePath("$.status");
    }

    @ParameterizedTest()
    @MethodSource("invalidPayloadOnUpdateRequest")
    void shouldReturnBadRequestForInvalidRequestOnTransactionUpdate(final String sid, final UpdateTransactionRequestDTO requestDTO) {

        final MvcTestResult testResult = mvc.patch()
                .uri("/transactions/{transactionSid}", sid)
                .header("X-User-Sid", UUID.randomUUID())
                .content(objectMapper.writeValueAsString(requestDTO))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange();

        assertThat(testResult)
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    private static Transaction getTransaction(final UUID transactionSid) {
        final BigDecimal amount = BigDecimal.TEN;
        final String categoryDisplayName = "Gas";
        final String description = "Diesel for car 1";

        final Category mockCategory = new Category();
        mockCategory.setDisplayName(categoryDisplayName);

        final Transaction mockTransaction = new Transaction();
        mockTransaction.setSid(transactionSid);
        mockTransaction.setAmount(amount);
        mockTransaction.setType(TransactionType.EXPENSE);
        mockTransaction.setDescription(description);
        mockTransaction.setCategory(mockCategory);
        return mockTransaction;
    }

    private static Stream<Arguments> invalidPayloadOnUpdateRequest() {
        return Stream.of(
                Arguments.of("invalid_sid", new UpdateTransactionRequestDTO(null, null, null, null, null)),
                Arguments.of(UUID.randomUUID().toString(), new UpdateTransactionRequestDTO(null, null, BigDecimal.ZERO, null, null)),
                Arguments.of(UUID.randomUUID().toString(), new UpdateTransactionRequestDTO(null, null, BigDecimal.TEN.negate(), null, null)),
                Arguments.of(UUID.randomUUID().toString(), new UpdateTransactionRequestDTO(null, null, null, null, LocalDateTime.now().plusDays(1))),
                Arguments.of(UUID.randomUUID().toString(), null)
        );
    }

}
