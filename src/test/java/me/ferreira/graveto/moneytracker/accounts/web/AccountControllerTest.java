package me.ferreira.graveto.moneytracker.accounts.web;

import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.accounts.service.command.CreateAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.web.dto.request.CreateAccountRequestDTO;
import me.ferreira.graveto.moneytracker.utils.common.ControllerUtils;
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

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = AccountController.class)
public class AccountControllerTest {

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private AccountService service;

    @Test
    void shouldCreateAccountSuccessfully() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final UUID accountSid = UUID.randomUUID();
        final BigDecimal initialBalance = BigDecimal.TEN;

        final CreateAccountRequestDTO request = new CreateAccountRequestDTO(
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
                .header("X-User-Sid", userSid)
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
                .extractingPath("$.status").asString().isEqualTo("ACTIVE");
    }

    @ParameterizedTest
    @MethodSource("invalidAccountCreationRequests")
    void shouldReturnBadRequestForInvalidPayloads(
            final CreateAccountRequestDTO request,
            final String expectedErrorField) {

        final MvcTestResult testResult = mvc.post()
                .uri("/accounts")
                .content(ControllerUtils.asJsonString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Sid", UUID.randomUUID())
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        assertThat(testResult)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPath("$.invalid_params." + expectedErrorField);
    }

    private static Stream<Arguments> invalidAccountCreationRequests() {
        return Stream.of(
                Arguments.of(new CreateAccountRequestDTO(BigDecimal.valueOf(-10), Currency.EUR, "Santander"), "initialBalance"),
                Arguments.of(new CreateAccountRequestDTO(BigDecimal.TEN, null, "Santander"), "currency"),
                Arguments.of(new CreateAccountRequestDTO(BigDecimal.TEN, Currency.EUR, ""), "institution"),
                Arguments.of(new CreateAccountRequestDTO(null, Currency.EUR, "Santander"), "initialBalance")
        );
    }

}
