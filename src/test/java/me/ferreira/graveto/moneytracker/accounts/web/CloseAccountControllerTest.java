package me.ferreira.graveto.moneytracker.accounts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.util.UUID;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.accounts.service.command.CloseAccountCommand;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import org.junit.jupiter.api.Test;
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
public class CloseAccountControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private AccountService service;

  @Test
  void shouldCloseAccountSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();

    final Account mockAccount =
        AccountUtils.createAccount(UUID.randomUUID(), userSid, BigDecimal.TEN, MembershipRole.OWNER);
    mockAccount.setStatus(AccountStatus.CLOSED);

    final ArgumentCaptor<CloseAccountCommand> commandCaptor = ArgumentCaptor.forClass(CloseAccountCommand.class);
    when(service.closeAccount(commandCaptor.capture())).thenReturn(mockAccount);

    // Act
    final MvcTestResult testResult = mvc.patch()
        .uri("/accounts/{accountSid}/close", mockAccount.getSid())
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.OK);

    final CloseAccountCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.accountSid()).isEqualTo(mockAccount.getSid());
    assertThat(capturedCommand.userSid()).isEqualTo(userSid);

    assertThat(testResult).bodyJson()
        .extractingPath("$.sid").asString().isEqualTo(mockAccount.getSid().toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$.balance").asNumber().isEqualTo(mockAccount.getBalance().intValue());
    assertThat(testResult).bodyJson()
        .extractingPath("$.baseCurrency").isEqualTo(mockAccount.getBaseCurrency().name());
    assertThat(testResult).bodyJson()
        .extractingPath("$.status").isEqualTo(AccountStatus.CLOSED.name());
    assertThat(testResult).bodyJson()
        .extractingPath("$.institution").isEqualTo(mockAccount.getInstitution());
    assertThat(testResult).bodyJson()
        .extractingPath("$.users[0].sid").isEqualTo(mockAccount.getMemberships().get(0).getUserSid().toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$.users[0].role").asString().isEqualTo(mockAccount.getMemberships().get(0).getRole().name());
  }

  @Test
  void shouldReturnBadRequestForInvalidRequestOnAccountClosure() {

    final MvcTestResult testResult = mvc.patch()
        .uri("/accounts/{accountSid}/close", "invalid_sid")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    assertThat(testResult)
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

}
