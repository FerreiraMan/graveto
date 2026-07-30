package me.ferreira.graveto.moneytracker.transactions.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.DeleteTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.web.helper.TransactionDtoAssertions;
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
    controllers = TransactionController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class DeleteTransactionControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private TransactionService service;

  @Test
  void shouldReturnDeletedTransactionAndMapToResponseDto() {
    // Arrange
    final UUID userSid = UUID.randomUUID();

    final Account mockAccount = new Account();
    mockAccount.setSid(UUID.randomUUID());
    mockAccount.setInstitution("BCP");
    mockAccount.setBaseCurrency(Currency.EUR);
    final Category mockCategory = new Category();
    mockCategory.setSid(UUID.randomUUID());
    mockCategory.setDisplayName("Groceries");
    final Transaction mockTransaction =
        Transaction.create(mockAccount, BigDecimal.ONE, null, mockCategory, TransactionType.EXPENSE,
            LocalDateTime.of(2020, 2, 3, 2, 10));

    final ArgumentCaptor<DeleteTransactionCommand> commandCaptor =
        ArgumentCaptor.forClass(DeleteTransactionCommand.class);
    when(service.deleteTransaction(commandCaptor.capture())).thenReturn(mockTransaction);

    // Act
    final MvcTestResult testResult = mvc.delete()
        .uri("/transactions/{sid}", mockTransaction.getSid())
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.OK);

    final DeleteTransactionCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.userSid()).isEqualTo(userSid);
    assertThat(capturedCommand.transactionSid()).isEqualTo(mockTransaction.getSid());

    TransactionDtoAssertions.assertSingleResponse(testResult, mockTransaction);
  }

  @Test
  void shouldReturnBadRequestForInvalidRequestOnTransactionDelete() {

    final MvcTestResult testResult = mvc.delete()
        .uri("/transactions/{transactionSid}", "invalid_sid")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    assertThat(testResult)
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

}
