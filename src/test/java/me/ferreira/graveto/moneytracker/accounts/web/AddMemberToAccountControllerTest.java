package me.ferreira.graveto.moneytracker.accounts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.accounts.service.command.AddMemberToAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.payload.AccountDetails;
import me.ferreira.graveto.moneytracker.accounts.web.dto.request.AddMemberToAccountRequestDto;
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
public class AddMemberToAccountControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private AccountService service;

  @Test
  void shouldAddMemberToAccountSuccessfully() {
    // Arrange
    final UUID accountSid = UUID.randomUUID();
    final UUID userSid = UUID.randomUUID();
    final UUID secondUserSid = UUID.randomUUID();
    final BigDecimal balance = BigDecimal.TEN;
    final String institution = "Santander";
    final String email = "email@test.com";

    final AccountDetails.MembershipDetails originalUser = new AccountDetails.MembershipDetails(
        userSid, "email_first_user@test.com", MembershipRole.OWNER.name()
    );

    final AccountDetails.MembershipDetails secondUser = new AccountDetails.MembershipDetails(
        secondUserSid, email, MembershipRole.CONTRIBUTOR.name()
    );

    final AddMemberToAccountRequestDto request = new AddMemberToAccountRequestDto(
        email, MembershipRole.CONTRIBUTOR
    );

    final AccountDetails accountDetails = new AccountDetails(
        accountSid,
        balance,
        Currency.EUR,
        AccountStatus.ACTIVE,
        institution,
        List.of(originalUser, secondUser)
    );

    final ArgumentCaptor<AddMemberToAccountCommand> commandCaptor =
        ArgumentCaptor.forClass(AddMemberToAccountCommand.class);
    when(service.addMember(commandCaptor.capture())).thenReturn(accountDetails);

    // Act
    final MvcTestResult testResult = mvc.post()
        .uri("/accounts/{accountSid}/memberships", accountSid)
        .content(ControllerUtils.asJsonString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.OK);

    final AddMemberToAccountCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.accountSid()).isEqualTo(accountSid);
    assertThat(capturedCommand.userSid()).isEqualTo(userSid);
    assertThat(capturedCommand.email()).isEqualTo(email);
    assertThat(capturedCommand.role()).isEqualTo(MembershipRole.CONTRIBUTOR);

    assertThat(testResult).bodyJson()
        .extractingPath("$.sid").asString().isEqualTo(accountSid.toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$.users[0].sid").asString().isEqualTo(userSid.toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$.users[1].sid").asString().isEqualTo(secondUserSid.toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$.users[1].email").asString().isEqualTo(email);
  }

  @Test
  void shouldReturnBadRequestForInvalidRequestOnAddingMemberToAccount() {

    final MvcTestResult testResult = mvc.post()
        .uri("/accounts/{accountSid}/memberships", "invalid_sid")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    assertThat(testResult)
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

  @ParameterizedTest
  @MethodSource("invalidAddMemberRequests")
  void shouldReturnBadRequestForInvalidPayloadsOnAddingMemberToAccount(
      final AddMemberToAccountRequestDto request,
      final String expectedErrorField) {

    final MvcTestResult testResult = mvc.post()
        .uri("/accounts/{accountSid}/memberships", UUID.randomUUID())
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

  private static Stream<Arguments> invalidAddMemberRequests() {
    return Stream.of(
        Arguments.of(new AddMemberToAccountRequestDto(null, MembershipRole.OWNER), "email"),
        Arguments.of(new AddMemberToAccountRequestDto("   ", MembershipRole.OWNER), "email"),
        Arguments.of(new AddMemberToAccountRequestDto("", MembershipRole.OWNER), "email"),
        Arguments.of(new AddMemberToAccountRequestDto("email@test.com", null), "role")
    );
  }

}
