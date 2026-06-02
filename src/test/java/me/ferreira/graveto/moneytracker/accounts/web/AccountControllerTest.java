package me.ferreira.graveto.moneytracker.accounts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.accounts.service.command.CreateAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.command.FetchAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.payload.AccountDetails;
import me.ferreira.graveto.moneytracker.accounts.web.dto.request.CreateAccountRequestDto;
import me.ferreira.graveto.moneytracker.utils.common.AuthUtils;
import me.ferreira.graveto.moneytracker.utils.common.ControllerUtils;
import me.ferreira.graveto.moneytracker.utils.common.TestSecurityConfig;
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

@WebMvcTest(
    controllers = AccountController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class AccountControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private AccountService service;

  private static Stream<Arguments> invalidAccountCreationRequests() {
    return Stream.of(
        Arguments.of(new CreateAccountRequestDto(BigDecimal.valueOf(-10), Currency.EUR, "Santander"), "initialBalance"),
        Arguments.of(new CreateAccountRequestDto(BigDecimal.TEN, null, "Santander"), "currency"),
        Arguments.of(new CreateAccountRequestDto(BigDecimal.TEN, Currency.EUR, ""), "institution"),
        Arguments.of(new CreateAccountRequestDto(null, Currency.EUR, "Santander"), "initialBalance")
    );
  }

  @Test
  void shouldCreateAccountSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final BigDecimal initialBalance = BigDecimal.TEN;

    final CreateAccountRequestDto request = new CreateAccountRequestDto(
        initialBalance,
        Currency.EUR,
        "Santander"
    );

    final Account mockAccount = new Account();
    mockAccount.setSid(accountSid);
    mockAccount.setStatus(AccountStatus.ACTIVE);

    final ArgumentCaptor<CreateAccountCommand> commandCaptor = ArgumentCaptor.forClass(CreateAccountCommand.class);
    when(service.createAccount(commandCaptor.capture())).thenReturn(mockAccount);

    // Act
    final MvcTestResult testResult = mvc.post()
        .uri("/accounts")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(ControllerUtils.asJsonString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.CREATED);
    assertThat(testResult).hasHeader("Location", "http://localhost/accounts/" + accountSid);

    final CreateAccountCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.userSid()).isEqualTo(userSid);
    assertThat(capturedCommand.baseCurrency()).isEqualTo(Currency.EUR);
    assertThat(capturedCommand.initialBalance()).isEqualByComparingTo(initialBalance);

    assertThat(testResult).bodyJson()
        .extractingPath("$.sid").asString().isEqualTo(accountSid.toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$.status").asString().isEqualTo(AccountStatus.ACTIVE.name());
  }

  @ParameterizedTest
  @MethodSource("invalidAccountCreationRequests")
  void shouldReturnBadRequestForInvalidPayloadsOnAccountCreation(
      final CreateAccountRequestDto request,
      final String expectedErrorField) {

    final MvcTestResult testResult = mvc.post()
        .uri("/accounts")
        .content(ControllerUtils.asJsonString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    assertThat(testResult)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .hasPath("$.invalid_params." + expectedErrorField);
  }

  @Test
  void shouldFetchAccountSuccessfully() {
    // Arrange
    final UUID accountSid = UUID.randomUUID();
    final UUID userSid = UUID.randomUUID();
    final BigDecimal balance = BigDecimal.TEN;
    final String institution = "Santander";

    final AccountDetails.MembershipDetails membershipDetails = new AccountDetails.MembershipDetails(
        userSid, "", MembershipRole.OWNER.name()
    );

    final AccountDetails accountDetails = new AccountDetails(
        accountSid,
        balance,
        Currency.EUR,
        AccountStatus.ACTIVE,
        institution,
        List.of(membershipDetails)
    );

    final ArgumentCaptor<FetchAccountCommand> commandCaptor = ArgumentCaptor.forClass(FetchAccountCommand.class);
    when(service.fetchAccount(commandCaptor.capture())).thenReturn(accountDetails);

    // Act
    final MvcTestResult testResult = mvc.get()
        .uri("/accounts/{accountSid}", accountSid)
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.OK);

    final FetchAccountCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.accountSid()).isEqualTo(accountSid);
    assertThat(capturedCommand.userSid()).isEqualTo(userSid);

    assertThat(testResult).bodyJson()
        .extractingPath("$.sid").asString().isEqualTo(accountSid.toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$.status").asString().isEqualTo(AccountStatus.ACTIVE.name());
    assertThat(testResult).bodyJson()
        .extractingPath("$.institution").asString().isEqualTo(institution);
    assertThat(testResult).bodyJson()
        .extractingPath("$.baseCurrency").asString().isEqualTo(Currency.EUR.name());
    assertThat(testResult).bodyJson()
        .extractingPath("$.balance").asNumber().isEqualTo(balance.intValue());
    assertThat(testResult).bodyJson()
        .extractingPath("$.users[0].sid").asString().isEqualTo(userSid.toString());
  }

  @Test
  void shouldReturnBadRequestForInvalidRequestOnAccountFetch() {

    final MvcTestResult testResult = mvc.get()
        .uri("/accounts/{accountSid}", "invalid_sid")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    assertThat(testResult)
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldThrowNotFoundOnAccountFetch() {

    final UUID accountSid = UUID.randomUUID();

    when(service.fetchAccount(any())).thenThrow(new AccountNotFoundException(accountSid));

    final MvcTestResult testResult = mvc.get()
        .uri("/accounts/{accountSid}", accountSid)
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    assertThat(testResult)
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.detail").asString()
        .isEqualTo("Account with SID [%s] was not found or you do not have permission to view it.", accountSid);
  }

  @Test
  void shouldFetchAllAccountsSuccessfully() {
    // Arrange
    final UUID accountSid = UUID.randomUUID();
    final UUID userSid = UUID.randomUUID();
    final BigDecimal balance = BigDecimal.TEN;
    final String institution = "Santander";

    final Account mockAccount = new Account();
    mockAccount.setSid(accountSid);
    mockAccount.setBalance(balance);
    mockAccount.setBaseCurrency(Currency.EUR);
    mockAccount.setInstitution(institution);
    mockAccount.setStatus(AccountStatus.ACTIVE);

    when(service.fetchAllAccounts(userSid)).thenReturn(List.of(mockAccount));

    // Act
    final MvcTestResult testResult = mvc.get()
        .uri("/accounts")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.OK);
    assertThat(testResult).bodyJson()
        .extractingPath("$[0].sid").asString().isEqualTo(accountSid.toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$[0].status").asString().isEqualTo(AccountStatus.ACTIVE.name());
    assertThat(testResult).bodyJson()
        .extractingPath("$[0].institution").asString().isEqualTo(institution);
    assertThat(testResult).bodyJson()
        .extractingPath("$[0].baseCurrency").asString().isEqualTo(Currency.EUR.name());
    assertThat(testResult).bodyJson()
        .extractingPath("$[0].balance").asNumber().isEqualTo(balance.intValue());
  }

}
