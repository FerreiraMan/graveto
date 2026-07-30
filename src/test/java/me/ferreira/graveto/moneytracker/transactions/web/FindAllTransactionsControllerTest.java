package me.ferreira.graveto.moneytracker.transactions.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction_;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.FindAllTransactionsCommand;
import me.ferreira.graveto.moneytracker.transactions.web.helper.TransactionDtoAssertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
public class FindAllTransactionsControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private TransactionService service;

  @Test
  void shouldReturnPaginatedTransactionsAndMapToResponseDto() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID categorySid = UUID.randomUUID();
    final LocalDate startDate = LocalDate.of(2025, 1, 1);
    final LocalDate endDate = LocalDate.of(2025, 12, 31);

    final LocalDateTime occurredAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    final Account mockAccount = new Account();
    mockAccount.setSid(accountSid);
    mockAccount.setInstitution("BCP");
    mockAccount.setBaseCurrency(Currency.EUR);
    final Category mockCategory = new Category();
    mockCategory.setSid(categorySid);
    mockCategory.setDisplayName("Groceries");
    final Transaction mockTransaction =
        Transaction.create(mockAccount, BigDecimal.ONE, "Supermarket", mockCategory, TransactionType.EXPENSE,
            occurredAt);

    final Page<Transaction> mockPage = new PageImpl<>(List.of(mockTransaction));

    final ArgumentCaptor<FindAllTransactionsCommand> commandCaptor =
        ArgumentCaptor.forClass(FindAllTransactionsCommand.class);
    when(service.findAll(commandCaptor.capture())).thenReturn(mockPage);

    // Act
    final MvcTestResult testResult = mvc.get()
        .uri("/transactions")
        .param("accountSid", String.valueOf(accountSid))
        .param("categorySid", String.valueOf(categorySid))
        .param("startDate", startDate.toString())
        .param("endDate", endDate.toString())
        .param("type", TransactionType.EXPENSE.name())
        .param("page", String.valueOf(1))
        .param("size", String.valueOf(10))
        .param("sort", "amount,asc")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.OK);
    TransactionDtoAssertions.assertPageableResponse(testResult, mockTransaction, 0);

    final FindAllTransactionsCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.userSid()).isEqualTo(userSid);
    assertThat(capturedCommand.accountSid()).isEqualTo(accountSid);
    assertThat(capturedCommand.categorySid()).isEqualTo(categorySid);
    assertThat(capturedCommand.startDate()).isEqualTo(startDate);
    assertThat(capturedCommand.endDate()).isEqualTo(endDate);
    assertThat(capturedCommand.type()).isEqualTo(TransactionType.EXPENSE);

    assertThat(capturedCommand.pageable().getPageNumber()).isEqualTo(1);
    assertThat(capturedCommand.pageable().getPageSize()).isEqualTo(10);
    assertThat(capturedCommand.pageable().getSort().getOrderFor("amount").getDirection()).isEqualTo(Sort.Direction.ASC);
  }

  @Test
  void shouldApplyDefaultPaginationAndSortingWhenNotProvided() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();

    final Page<Transaction> mockPage = new PageImpl<>(Collections.emptyList());

    final ArgumentCaptor<FindAllTransactionsCommand> commandCaptor =
        ArgumentCaptor.forClass(FindAllTransactionsCommand.class);
    when(service.findAll(commandCaptor.capture())).thenReturn(mockPage);

    // Act
    final MvcTestResult testResult = mvc.get()
        .uri("/transactions")
        .param("accountSid", String.valueOf(accountSid))
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.OK);

    final FindAllTransactionsCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.accountSid()).isEqualTo(accountSid);
    assertThat(capturedCommand.categorySid()).isNull();

    assertThat(capturedCommand.pageable().getPageSize()).isEqualTo(20);
    assertThat(capturedCommand.pageable().getSort().getOrderFor(Transaction_.OCCURRED_AT).getDirection()).isEqualTo(
        Sort.Direction.DESC);
  }

  @Test
  void shouldReturnBadRequestWhenAccountSidIsMissing() {
    // Act
    final MvcTestResult testResult = mvc.get()
        .uri("/transactions")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .hasPath("$.invalid_params.accountSid");
  }

}
