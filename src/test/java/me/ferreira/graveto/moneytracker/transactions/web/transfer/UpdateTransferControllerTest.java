package me.ferreira.graveto.moneytracker.transactions.web.transfer;

import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.UpdateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.TransferService;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import me.ferreira.graveto.moneytracker.transactions.web.TransferController;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transfer.UpdateTransferRequestDTO;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = TransferController.class)
public class UpdateTransferControllerTest {

    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TransferService service;

    @Test
    void shouldReturnUpdatedTransferAndMapToResponseDTO() {
        // Arrange
        final Account sourceAccount = AccountUtils.createAccount(BigDecimal.TEN);
        final Account destinationAccount = AccountUtils.createAccount(BigDecimal.TEN);
        final UUID userSid = UUID.randomUUID();
        final UUID correlationId = UUID.randomUUID();
        final LocalDateTime newOccurredAt = LocalDateTime.now();

        final UpdateTransferRequestDTO requestDTO = new UpdateTransferRequestDTO(
                BigDecimal.TEN,
                "Updated transfer description",
                newOccurredAt
        );

        final Transaction mockExpense = new Transaction();
        mockExpense.setCorrelationId(correlationId);
        mockExpense.setAccount(sourceAccount);
        final Transaction mockIncome = new Transaction();
        mockIncome.setAccount(destinationAccount);

        final TransferResult mockResult = new TransferResult(mockExpense, mockIncome);

        final ArgumentCaptor<UpdateTransferCommand> commandCaptor = ArgumentCaptor.forClass(UpdateTransferCommand.class);
        when(service.updateTransfer(commandCaptor.capture())).thenReturn(mockResult);

        // Act
        final MvcTestResult testResult = mvc.patch()
                .uri("/transfers/{correlationId}", correlationId)
                .header("X-User-Sid", userSid)
                .content(objectMapper.writeValueAsString(requestDTO))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange();

        // Assert
        assertThat(testResult).hasStatus(HttpStatus.OK);

        final UpdateTransferCommand capturedCommand = commandCaptor.getValue();
        assertThat(capturedCommand.userSid()).isEqualTo(userSid);
        assertThat(capturedCommand.correlationId()).isEqualTo(correlationId);
        assertThat(capturedCommand.amount()).isEqualByComparingTo(requestDTO.amount());
        assertThat(capturedCommand.description()).isEqualTo(requestDTO.description());
        assertThat(capturedCommand.occurredAt()).isEqualTo(requestDTO.occurredAt());

        assertThat(testResult).bodyJson()
                .extractingPath("$.correlationId").asString().isEqualTo(correlationId.toString());
    }

    @ParameterizedTest()
    @MethodSource("invalidPayloadOnUpdateRequest")
    void shouldReturnBadRequestForInvalidRequestOnTransferUpdate(final String correlationId, final UpdateTransferRequestDTO requestDTO) {

        final MvcTestResult testResult = mvc.patch()
                .uri("/transfers/{correlationId}", correlationId)
                .header("X-User-Sid", UUID.randomUUID())
                .content(objectMapper.writeValueAsString(requestDTO))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.BAD_REQUEST);
    }

    private static Stream<Arguments> invalidPayloadOnUpdateRequest() {
        return Stream.of(
                Arguments.of("invalid_sid", new UpdateTransferRequestDTO(null, null, null)),
                Arguments.of(UUID.randomUUID().toString(), new UpdateTransferRequestDTO(BigDecimal.ZERO, null, null)),
                Arguments.of(UUID.randomUUID().toString(), new UpdateTransferRequestDTO(BigDecimal.TEN.negate(), null, null)),
                Arguments.of(UUID.randomUUID().toString(), new UpdateTransferRequestDTO(null, null, LocalDateTime.now().plusDays(1))),
                Arguments.of(UUID.randomUUID().toString(), null)
        );
    }

}
