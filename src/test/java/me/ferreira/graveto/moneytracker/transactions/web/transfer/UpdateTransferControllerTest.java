package me.ferreira.graveto.moneytracker.transactions.web.transfer;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.UpdateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.TransferService;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import me.ferreira.graveto.moneytracker.transactions.web.TransferController;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transfer.UpdateTransferRequestDto;
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
public class UpdateTransferControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private TransferService service;

  private static Stream<Arguments> invalidPayloadOnUpdateRequest() {
    return Stream.of(
        Arguments.of("invalid_sid", new UpdateTransferRequestDto(null, null, null)),
        Arguments.of(UUID.randomUUID().toString(), new UpdateTransferRequestDto(BigDecimal.ZERO, null, null)),
        Arguments.of(UUID.randomUUID().toString(), new UpdateTransferRequestDto(BigDecimal.TEN.negate(), null, null)),
        Arguments.of(UUID.randomUUID().toString(),
            new UpdateTransferRequestDto(null, null, LocalDateTime.now().plusDays(1))),
        Arguments.of(UUID.randomUUID().toString(), null)
    );
  }

  @Test
  void shouldReturnUpdatedTransferAndMapToResponseDto() {
    // Arrange
    final Account sourceAccount = AccountUtils.createAccount(BigDecimal.TEN);
    final Account destinationAccount = AccountUtils.createAccount(BigDecimal.TEN);
    final UUID userSid = UUID.randomUUID();
    final UUID correlationId = UUID.randomUUID();
    final LocalDateTime newOccurredAt = LocalDateTime.now();

    final UpdateTransferRequestDto requestDto = new UpdateTransferRequestDto(
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
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(requestDto))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.OK);

    final UpdateTransferCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.userSid()).isEqualTo(userSid);
    assertThat(capturedCommand.correlationId()).isEqualTo(correlationId);
    assertThat(capturedCommand.amount()).isEqualByComparingTo(requestDto.amount());
    assertThat(capturedCommand.description()).isEqualTo(requestDto.description());
    assertThat(capturedCommand.occurredAt()).isEqualTo(requestDto.occurredAt());

    assertThat(testResult).bodyJson()
        .extractingPath("$.correlationId").asString().isEqualTo(correlationId.toString());
  }

  @ParameterizedTest()
  @MethodSource("invalidPayloadOnUpdateRequest")
  void shouldReturnBadRequestForInvalidRequestOnTransferUpdate(final String correlationId,
                                                               final UpdateTransferRequestDto requestDto) {

    final MvcTestResult testResult = mvc.patch()
        .uri("/transfers/{correlationId}", correlationId)
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .content(objectMapper.writeValueAsString(requestDto))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    assertThat(testResult).hasStatus(HttpStatus.BAD_REQUEST);
  }

}
