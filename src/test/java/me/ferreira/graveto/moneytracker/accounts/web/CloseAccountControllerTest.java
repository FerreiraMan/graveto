package me.ferreira.graveto.moneytracker.accounts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.accounts.service.command.CloseAccountCommand;
import me.ferreira.graveto.moneytracker.utils.common.AuthUtils;
import me.ferreira.graveto.moneytracker.utils.common.TestSecurityConfig;
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
    final UUID accountSid = UUID.randomUUID();

    final Account mockAccount = new Account();
    mockAccount.setSid(accountSid);
    mockAccount.setStatus(AccountStatus.CLOSED);

    final ArgumentCaptor<CloseAccountCommand> commandCaptor = ArgumentCaptor.forClass(CloseAccountCommand.class);
    when(service.closeAccount(commandCaptor.capture())).thenReturn(mockAccount);

    // Act
    final MvcTestResult testResult = mvc.patch()
        .uri("/accounts/{accountSid}/close", accountSid)
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.OK);

    final CloseAccountCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.accountSid()).isEqualTo(accountSid);
    assertThat(capturedCommand.userSid()).isEqualTo(userSid);

    assertThat(testResult).bodyJson()
        .extractingPath("$.sid").asString().isEqualTo(accountSid.toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$.status").asString().isEqualTo(AccountStatus.CLOSED.name());
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
