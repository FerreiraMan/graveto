package me.ferreira.graveto.moneytracker.transactions.web.recurringtransfers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.service.RecurringTransferService;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.FindAllRecurringTransfersCommand;
import me.ferreira.graveto.moneytracker.transactions.web.RecurringTransferController;
import me.ferreira.graveto.moneytracker.transactions.web.helper.RecurringTransferDtoAssertions;
import org.assertj.core.api.InstanceOfAssertFactories;
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

@WebMvcTest(
    controllers = RecurringTransferController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class FindAllRecurringTransfersControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private RecurringTransferService service;

  @Test
  void shouldFetchAllRecurringTransfersSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final RecurringTransfer rt = buildMockRecurringTransfer();

    final ArgumentCaptor<FindAllRecurringTransfersCommand> commandCaptor =
        ArgumentCaptor.forClass(FindAllRecurringTransfersCommand.class);
    when(service.fetchAllRecurringTransfers(commandCaptor.capture())).thenReturn(List.of(rt));

    // Act
    final MvcTestResult result =
        mvc.get().uri("/recurring-transfers").with(authentication(AuthUtils.mockAuth(userSid))).exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);

    final FindAllRecurringTransfersCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.status()).isNull();
    assertThat(captured.sourceAccountSid()).isNull();
    assertThat(captured.destinationAccountSid()).isNull();

    RecurringTransferDtoAssertions.assertListResponse(result, rt, 0);
  }

  @Test
  void shouldPassStatusFilterToCommand() {
    // Arrange
    final UUID userSid = UUID.randomUUID();

    final ArgumentCaptor<FindAllRecurringTransfersCommand> commandCaptor =
        ArgumentCaptor.forClass(FindAllRecurringTransfersCommand.class);
    when(service.fetchAllRecurringTransfers(commandCaptor.capture())).thenReturn(List.of());

    // Act
    final MvcTestResult result = mvc.get().uri("/recurring-transfers").param("status", "ACTIVE")
        .with(authentication(AuthUtils.mockAuth(userSid))).exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(commandCaptor.getValue().status()).isEqualTo(RecurringOperationStatus.ACTIVE);
  }

  @Test
  void shouldPassAccountsSidsFilterToCommand() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID sourceAccountSid = UUID.randomUUID();
    final UUID destinationAccountSid = UUID.randomUUID();

    final ArgumentCaptor<FindAllRecurringTransfersCommand> commandCaptor =
        ArgumentCaptor.forClass(FindAllRecurringTransfersCommand.class);
    when(service.fetchAllRecurringTransfers(commandCaptor.capture())).thenReturn(List.of());

    // Act
    final MvcTestResult result =
        mvc.get().uri("/recurring-transfers").param("sourceAccountSid", sourceAccountSid.toString())
            .param("destinationAccountSid", destinationAccountSid.toString())
            .with(authentication(AuthUtils.mockAuth(userSid))).exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(commandCaptor.getValue().sourceAccountSid()).isEqualTo(sourceAccountSid);
    assertThat(commandCaptor.getValue().destinationAccountSid()).isEqualTo(destinationAccountSid);
  }

  @Test
  void shouldReturnEmptyListWhenNoRecurringTransfersExist() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    when(service.fetchAllRecurringTransfers(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

    // Act
    final MvcTestResult result =
        mvc.get().uri("/recurring-transfers").with(authentication(AuthUtils.mockAuth(userSid))).exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(result).bodyJson().extractingPath("$").asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty();
  }

  private RecurringTransfer buildMockRecurringTransfer() {
    final Account sourceAccount = new Account();
    sourceAccount.setSid(UUID.randomUUID());
    sourceAccount.setInstitution("Santander");
    sourceAccount.setBaseCurrency(Currency.EUR);

    final Account destinationAccount = new Account();
    destinationAccount.setSid(UUID.randomUUID());
    destinationAccount.setInstitution("BCP");
    destinationAccount.setBaseCurrency(Currency.EUR);

    final RecurringTransfer rt = new RecurringTransfer();
    rt.setSid(UUID.randomUUID());
    rt.setSourceAccount(sourceAccount);
    rt.setDestinationAccount(destinationAccount);
    rt.setUserSid(UUID.randomUUID());
    rt.setDescription("Home Insurance");
    rt.setAmount(new BigDecimal("50"));
    rt.setCurrency(Currency.EUR);
    rt.setFrequency(Frequency.MONTHLY);
    rt.setDayOfTheMonth(15);
    rt.setAdjustToBusinessDay(true);
    rt.setNextExecutionDate(LocalDate.of(2026, 8, 15));
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    rt.setEndDate(null);
    return rt;
  }

}
