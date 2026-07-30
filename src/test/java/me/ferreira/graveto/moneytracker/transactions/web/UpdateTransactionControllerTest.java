package me.ferreira.graveto.moneytracker.transactions.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.UpdateTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.UpdateTransactionRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.helper.TransactionDtoAssertions;
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
    controllers = TransactionController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class UpdateTransactionControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private TransactionService service;

  private static Stream<Arguments> invalidPayloadOnUpdateRequest() {
    return Stream.of(
        Arguments.of("invalid_sid", new UpdateTransactionRequestDto(null, null, null, null, null)),
        Arguments.of(UUID.randomUUID().toString(),
            new UpdateTransactionRequestDto(null, null, BigDecimal.ZERO, null, null)),
        Arguments.of(UUID.randomUUID().toString(),
            new UpdateTransactionRequestDto(null, null, BigDecimal.TEN.negate(), null, null)),
        Arguments.of(UUID.randomUUID().toString(),
            new UpdateTransactionRequestDto(null, null, null, null, LocalDateTime.now().plusDays(1))),
        Arguments.of(UUID.randomUUID().toString(), null)
    );
  }

  @Test
  void shouldReturnUpdatedTransactionAndMapToResponseDto() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID categorySid = UUID.randomUUID();
    final Account mockAccount = new Account();
    mockAccount.setSid(UUID.randomUUID());
    mockAccount.setInstitution("BCP");
    mockAccount.setBaseCurrency(Currency.EUR);
    final Category mockCategory = new Category();
    mockCategory.setSid(UUID.randomUUID());
    mockCategory.setDisplayName("Gas");
    final Transaction mockTransaction =
        Transaction.create(mockAccount, BigDecimal.TEN, "Diesel for car 1", mockCategory, TransactionType.EXPENSE,
            LocalDateTime.of(2020, 2, 3, 2, 10));
    final LocalDateTime newOccurredAt = LocalDateTime.now();

    final UpdateTransactionRequestDto requestDto = new UpdateTransactionRequestDto(
        TransactionType.EXPENSE,
        categorySid,
        BigDecimal.TEN,
        "Diesel for car 2",
        newOccurredAt
    );

    final ArgumentCaptor<UpdateTransactionCommand> commandCaptor =
        ArgumentCaptor.forClass(UpdateTransactionCommand.class);
    when(service.updateTransaction(commandCaptor.capture())).thenReturn(mockTransaction);

    // Act
    final MvcTestResult testResult = mvc.patch()
        .uri("/transactions/{sid}", mockTransaction.getSid())
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(requestDto))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.OK);

    final UpdateTransactionCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.userSid()).isEqualTo(userSid);
    assertThat(capturedCommand.transactionSid()).isEqualTo(mockTransaction.getSid());
    assertThat(capturedCommand.transactionType()).isEqualTo(requestDto.transactionType());
    assertThat(capturedCommand.categorySid()).isEqualTo(requestDto.categorySid());
    assertThat(capturedCommand.amount()).isEqualByComparingTo(requestDto.amount());
    assertThat(capturedCommand.description()).isEqualTo(requestDto.description());
    assertThat(capturedCommand.occurredAt()).isEqualTo(requestDto.occurredAt());

    TransactionDtoAssertions.assertSingleResponse(testResult, mockTransaction);
  }

  @ParameterizedTest()
  @MethodSource("invalidPayloadOnUpdateRequest")
  void shouldReturnBadRequestForInvalidRequestOnTransactionUpdate(final String sid,
                                                                  final UpdateTransactionRequestDto requestDto) {

    final MvcTestResult testResult = mvc.patch()
        .uri("/transactions/{transactionSid}", sid)
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .content(objectMapper.writeValueAsString(requestDto))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    assertThat(testResult)
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

}
