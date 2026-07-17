package me.ferreira.graveto.moneytracker.transactions.web.recurringtransactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.service.RecurringTransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.UpdateRecurringTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.web.RecurringTransactionController;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransaction.UpdateRecurringTransactionRequestDto;
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
    controllers = RecurringTransactionController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class UpdateRecurringTransactionControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private RecurringTransactionService service;

  private static Stream<Arguments> invalidRequests() {
    return Stream.of(
        Arguments.of(new UpdateRecurringTransactionRequestDto(
            "desc", BigDecimal.ZERO, Frequency.MONTHLY, 15, null, true,
            null, null, null), "amount"),
        Arguments.of(new UpdateRecurringTransactionRequestDto(
            "desc", BigDecimal.TEN.negate(), Frequency.MONTHLY, 15, null, true,
            null, null, null), "amount"),
        Arguments.of(new UpdateRecurringTransactionRequestDto(
            "desc", BigDecimal.TEN, Frequency.MONTHLY, 0, null, true,
            null, null, null), "dayOfMonth"),
        Arguments.of(new UpdateRecurringTransactionRequestDto(
            "desc", BigDecimal.TEN, Frequency.MONTHLY, 32, null, true,
            null, null, null), "dayOfMonth"),
        Arguments.of(new UpdateRecurringTransactionRequestDto(
            "desc", BigDecimal.TEN, Frequency.WEEKLY, null, 0, true,
            null, null, null), "dayOfWeek"),
        Arguments.of(new UpdateRecurringTransactionRequestDto(
            "desc", BigDecimal.TEN, Frequency.WEEKLY, null, 8, true,
            null, null, null), "dayOfWeek")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidRequests")
  void shouldReturnBadRequestForInvalidPayloads(
      final UpdateRecurringTransactionRequestDto request,
      final String expectedErrorField) {

    final MvcTestResult result = mvc.patch()
        .uri("/recurring-transactions/" + UUID.randomUUID())
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .hasPath("$.invalid_params." + expectedErrorField);
  }

  @Test
  void shouldUpdateRecurringTransactionSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();

    final UpdateRecurringTransactionRequestDto request = new UpdateRecurringTransactionRequestDto(
        "Updated Insurance", new BigDecimal("75.00"), Frequency.WEEKLY, null, 3, false,
        RecurringOperationStatus.ACTIVE, LocalDate.of(2026, 9, 1), LocalDate.of(2027, 9, 1));

    final RecurringTransaction mockRt = buildMockRecurringTransaction(rtSid, accountSid, userSid);
    mockRt.setDescription("Updated Insurance");
    mockRt.setAmount(new BigDecimal("75.00"));
    mockRt.setFrequency(Frequency.WEEKLY);
    mockRt.setDayOfTheWeek(3);
    mockRt.setAdjustToBusinessDay(false);
    mockRt.setNextExecutionDate(LocalDate.of(2026, 9, 1));

    final ArgumentCaptor<UpdateRecurringTransactionCommand> commandCaptor =
        ArgumentCaptor.forClass(UpdateRecurringTransactionCommand.class);
    when(service.updateRecurringTransaction(commandCaptor.capture())).thenReturn(mockRt);

    // Act
    final MvcTestResult result = mvc.patch()
        .uri("/recurring-transactions/" + rtSid)
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);

    final UpdateRecurringTransactionCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.sid()).isEqualTo(rtSid);
    assertThat(captured.description()).isEqualTo("Updated Insurance");
    assertThat(captured.amount()).isEqualByComparingTo(new BigDecimal("75.00"));
    assertThat(captured.frequency()).isEqualTo(Frequency.WEEKLY);
    assertThat(captured.dayOfWeek()).isEqualTo(3);
    assertThat(captured.adjustToBusinessDay()).isFalse();
    assertThat(captured.status()).isEqualTo(RecurringOperationStatus.ACTIVE);
    assertThat(captured.nextExecutionDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(captured.endDate()).isEqualTo(LocalDate.of(2027, 9, 1));

    assertThat(result).bodyJson().extractingPath("$.sid").asString().isEqualTo(rtSid.toString());
    assertThat(result).bodyJson().extractingPath("$.frequency").asString().isEqualTo("WEEKLY");
    assertThat(result).bodyJson().extractingPath("$.status").asString().isEqualTo("ACTIVE");
  }

  @Test
  void shouldAllowPartialUpdate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();

    final UpdateRecurringTransactionRequestDto request = new UpdateRecurringTransactionRequestDto(
        null, new BigDecimal("100.00"), null, null, null, null,
        null, null, null);

    final RecurringTransaction mockRt = buildMockRecurringTransaction(rtSid, accountSid, userSid);
    mockRt.setAmount(new BigDecimal("100.00"));

    final ArgumentCaptor<UpdateRecurringTransactionCommand> commandCaptor =
        ArgumentCaptor.forClass(UpdateRecurringTransactionCommand.class);
    when(service.updateRecurringTransaction(commandCaptor.capture())).thenReturn(mockRt);

    // Act
    final MvcTestResult result = mvc.patch()
        .uri("/recurring-transactions/" + rtSid)
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);

    final UpdateRecurringTransactionCommand captured = commandCaptor.getValue();
    assertThat(captured.description()).isNull();
    assertThat(captured.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
    assertThat(captured.frequency()).isNull();
    assertThat(captured.dayOfMonth()).isNull();
    assertThat(captured.dayOfWeek()).isNull();
    assertThat(captured.adjustToBusinessDay()).isNull();
    assertThat(captured.status()).isNull();
    assertThat(captured.nextExecutionDate()).isNull();
    assertThat(captured.endDate()).isNull();
  }

  @Test
  void shouldTrimDescriptionBeforePassingToService() {
    // Arrange
    final UUID accountSid = UUID.randomUUID();
    final UpdateRecurringTransactionRequestDto request = new UpdateRecurringTransactionRequestDto(
        "  Updated  ", null, null, null, null, null, null, null, null);

    final RecurringTransaction mockRt = buildMockRecurringTransaction(UUID.randomUUID(), accountSid, UUID.randomUUID());

    final ArgumentCaptor<UpdateRecurringTransactionCommand> commandCaptor =
        ArgumentCaptor.forClass(UpdateRecurringTransactionCommand.class);
    when(service.updateRecurringTransaction(commandCaptor.capture())).thenReturn(mockRt);

    // Act
    mvc.patch()
        .uri("/recurring-transactions/" + UUID.randomUUID())
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(commandCaptor.getValue().description()).isEqualTo("Updated");
  }

  @Test
  void shouldReturnBadRequestForInvalidSidPathVariable() {
    // Arrange
    final UpdateRecurringTransactionRequestDto request = new UpdateRecurringTransactionRequestDto(
        null, null, null, null, null, null, null, null, null);

    // Act
    final MvcTestResult result = mvc.patch()
        .uri("/recurring-transactions/not-a-uuid")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
  }

  private RecurringTransaction buildMockRecurringTransaction(final UUID rtSid, final UUID accountSid,
                                                             final UUID userSid) {
    final Account account = new Account();
    account.setSid(accountSid);
    account.setInstitution("Santander");
    account.setBaseCurrency(Currency.EUR);

    final Category category = new Category();
    category.setSid(UUID.randomUUID());
    category.setDisplayName("Insurance");

    final RecurringTransaction rt = new RecurringTransaction();
    rt.setSid(rtSid);
    rt.setAccount(account);
    rt.setCategory(category);
    rt.setUserSid(userSid);
    rt.setDescription("Home Insurance");
    rt.setAmount(new BigDecimal("50.00"));
    rt.setCurrency(Currency.EUR);
    rt.setType(TransactionType.EXPENSE);
    rt.setFrequency(Frequency.MONTHLY);
    rt.setDayOfTheMonth(15);
    rt.setAdjustToBusinessDay(true);
    rt.setNextExecutionDate(LocalDate.of(2026, 8, 15));
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    rt.setEndDate(null);
    return rt;
  }

}
