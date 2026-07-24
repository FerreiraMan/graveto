package me.ferreira.graveto.moneytracker.transactions.web.recurringtransfers;

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
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.service.RecurringTransferService;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.CreateRecurringTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.web.RecurringTransferController;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransfer.CreateRecurringTransferRequestDto;
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
    controllers = RecurringTransferController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class CreateRecurringTransferControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private RecurringTransferService service;

  private static Stream<Arguments> invalidRequests() {
    return Stream.of(
        Arguments.of(new CreateRecurringTransferRequestDto(
            null, UUID.randomUUID(), "desc", BigDecimal.TEN,
            Frequency.MONTHLY, 15, null, true, null, null), "sourceAccountSid"),
        Arguments.of(new CreateRecurringTransferRequestDto(
            UUID.randomUUID(), null, "desc", BigDecimal.TEN,
            Frequency.MONTHLY, 15, null, true, null, null), "destinationAccountSid"),
        Arguments.of(new CreateRecurringTransferRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", null,
            Frequency.MONTHLY, 15, null, true, null, null), "amount"),
        Arguments.of(new CreateRecurringTransferRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.ZERO,
            Frequency.MONTHLY, 15, null, true, null, null), "amount"),
        Arguments.of(new CreateRecurringTransferRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.TEN.negate(),
            Frequency.MONTHLY, 15, null, true, null, null), "amount"),
        Arguments.of(new CreateRecurringTransferRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.TEN,
            null, 15, null, true, null, null), "frequency"),
        Arguments.of(new CreateRecurringTransferRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.TEN,
            Frequency.MONTHLY, null, null, null, null, null), "adjustToBusinessDay"),
        Arguments.of(new CreateRecurringTransferRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.TEN,
            Frequency.MONTHLY, 0, null, true, null, null), "dayOfMonth"),
        Arguments.of(new CreateRecurringTransferRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.TEN,
            Frequency.MONTHLY, 32, null, true, null, null), "dayOfMonth"),
        Arguments.of(new CreateRecurringTransferRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.TEN,
            Frequency.WEEKLY, null, 0, true, null, null), "dayOfWeek"),
        Arguments.of(new CreateRecurringTransferRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), "desc", BigDecimal.TEN,
            Frequency.WEEKLY, null, 8, true, null, null), "dayOfWeek")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidRequests")
  void shouldReturnBadRequestForInvalidPayloads(
      final CreateRecurringTransferRequestDto request,
      final String expectedErrorField) {

    final MvcTestResult result = mvc.post()
        .uri("/recurring-transfers")
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
  void shouldCreateRecurringTransferSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID sourceAccountSid = UUID.randomUUID();
    final UUID destinationAccountSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final LocalDate nextExecution = LocalDate.of(2026, 8, 15);

    final CreateRecurringTransferCommand request = new CreateRecurringTransferCommand(userSid,
        sourceAccountSid, destinationAccountSid, "Home Insurance", new BigDecimal("50.00"),
        Frequency.MONTHLY, 15, null, true, null, null);

    final Account mockSourceAccount = new Account();
    mockSourceAccount.setSid(sourceAccountSid);
    mockSourceAccount.setInstitution("Santander");
    mockSourceAccount.setBaseCurrency(Currency.EUR);

    final Account mockDestinationAccount = new Account();
    mockDestinationAccount.setSid(destinationAccountSid);
    mockDestinationAccount.setInstitution("BCP");
    mockDestinationAccount.setBaseCurrency(Currency.EUR);

    final RecurringTransfer mockRt = new RecurringTransfer();
    mockRt.setSid(rtSid);
    mockRt.setSourceAccount(mockSourceAccount);
    mockRt.setDestinationAccount(mockDestinationAccount);
    mockRt.setUserSid(userSid);
    mockRt.setDescription("Home Insurance");
    mockRt.setAmount(new BigDecimal("50.00"));
    mockRt.setCurrency(Currency.EUR);
    mockRt.setFrequency(Frequency.MONTHLY);
    mockRt.setNextExecutionDate(nextExecution);
    mockRt.setStatus(RecurringOperationStatus.ACTIVE);
    mockRt.setEndDate(null);

    final ArgumentCaptor<CreateRecurringTransferCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateRecurringTransferCommand.class);
    when(service.createRecurringTransfer(commandCaptor.capture())).thenReturn(mockRt);

    // Act
    final MvcTestResult result = mvc.post()
        .uri("/recurring-transfers")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.CREATED);
    assertThat(result).hasHeader("Location", "http://localhost/recurring-transfers/" + rtSid);

    final CreateRecurringTransferCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.sourceAccountSid()).isEqualTo(sourceAccountSid);
    assertThat(captured.destinationAccountSid()).isEqualTo(destinationAccountSid);
    assertThat(captured.description()).isEqualTo("Home Insurance");
    assertThat(captured.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
    assertThat(captured.frequency()).isEqualTo(Frequency.MONTHLY);
    assertThat(captured.dayOfMonth()).isEqualTo(15);
    assertThat(captured.adjustToBusinessDay()).isTrue();

    assertThat(result).bodyJson().extractingPath("$.sid").asString().isEqualTo(rtSid.toString());
    assertThat(result).bodyJson().extractingPath("$.sourceAccount.name").asString().isEqualTo("Santander");
    assertThat(result).bodyJson().extractingPath("$.destinationAccount.name").asString().isEqualTo("BCP");
    assertThat(result).bodyJson().extractingPath("$.frequency").asString().isEqualTo("MONTHLY");
    assertThat(result).bodyJson().extractingPath("$.status").asString().isEqualTo("ACTIVE");
    assertThat(result).bodyJson().extractingPath("$.nextExecutionDate").asString().isEqualTo("2026-08-15");
  }

  @Test
  void shouldTrimDescriptionBeforePassingToService() {
    // Arrange
    final CreateRecurringTransferRequestDto request = new CreateRecurringTransferRequestDto(
        UUID.randomUUID(), UUID.randomUUID(), "  Home Insurance  ", new BigDecimal("50.00"), Frequency.MONTHLY, 15,
        null, true, null, null);

    final RecurringTransfer mockRt = buildMinimalMockRt();

    final ArgumentCaptor<CreateRecurringTransferCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateRecurringTransferCommand.class);
    when(service.createRecurringTransfer(commandCaptor.capture())).thenReturn(mockRt);

    // Act
    mvc.post()
        .uri("/recurring-transfers")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(commandCaptor.getValue().description()).isEqualTo("Home Insurance");
  }

  private RecurringTransfer buildMinimalMockRt() {
    final Account account = new Account();
    account.setSid(UUID.randomUUID());
    account.setInstitution("Santander");

    final Account account2 = new Account();
    account.setSid(UUID.randomUUID());
    account.setInstitution("Santander");

    final RecurringTransfer rt = new RecurringTransfer();
    rt.setSid(UUID.randomUUID());
    rt.setSourceAccount(account);
    rt.setDestinationAccount(account2);
    rt.setUserSid(UUID.randomUUID());
    rt.setDescription("Home Insurance");
    rt.setAmount(new BigDecimal("50.00"));
    rt.setCurrency(Currency.EUR);
    rt.setFrequency(Frequency.MONTHLY);
    rt.setNextExecutionDate(LocalDate.now().plusMonths(1));
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    rt.setEndDate(null);
    return rt;
  }

}
