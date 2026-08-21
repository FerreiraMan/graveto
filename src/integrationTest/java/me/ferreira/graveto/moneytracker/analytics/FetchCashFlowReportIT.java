package me.ferreira.graveto.moneytracker.analytics;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.categories.service.command.FindAllCategoriesCommand;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.CreateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.TransferService;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import me.ferreira.graveto.moneytracker.utils.TransactionTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FetchCashFlowReportIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private TransactionRepository transactionRepository;
  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private CategoryRepository categoryRepository;
  @Autowired
  private CategoryService categoryService;
  @Autowired
  private TransferService transferService;

  @Test
  void shouldFetchCashFlowReportWithAccurateAggregations() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final BigDecimal openingBalance = BigDecimal.valueOf(5000);
    final Account account =
        AccountTestFactory.createAccountWithOwner(userSid, "Main Checking", openingBalance);
    accountRepository.save(account);

    final Category incomeCategory = fetchCategory(userSid, TransactionType.INCOME);
    final Category expenseCategory = fetchCategory(userSid, TransactionType.EXPENSE);
    final Category openingBalanceCategory = fetchOpeningBalanceCategory();

    final int targetYear = 2026;

    final Transaction openingTx = TransactionTestFactory.createOpeningBalanceTransaction(
        account, openingBalanceCategory, openingBalance, LocalDate.of(targetYear, 1, 1));

    // Income
    final Transaction txJanIncome =
        TransactionTestFactory.createTransaction(account, incomeCategory, TransactionType.INCOME,
            BigDecimal.valueOf(1000), TransactionStatus.ACTIVE, LocalDate.of(targetYear, 1, 10));
    final Transaction txJanSecondIncome =
        TransactionTestFactory.createTransaction(account, incomeCategory, TransactionType.INCOME,
            BigDecimal.valueOf(230), TransactionStatus.ACTIVE, LocalDate.of(targetYear, 1, 10));
    final Transaction txFebIncome =
        TransactionTestFactory.createTransaction(account, incomeCategory, TransactionType.INCOME,
            BigDecimal.valueOf(500), TransactionStatus.ACTIVE, LocalDate.of(targetYear, 2, 10));

    // Expense
    final Transaction txJanExpense =
        TransactionTestFactory.createTransaction(account, expenseCategory, TransactionType.EXPENSE,
            BigDecimal.valueOf(200), TransactionStatus.ACTIVE, LocalDate.of(targetYear, 1, 15));

    // Deleted - should be ignored
    final Transaction txDeleted =
        TransactionTestFactory.createTransaction(account, expenseCategory, TransactionType.EXPENSE,
            BigDecimal.valueOf(9999), TransactionStatus.DELETED, LocalDate.of(targetYear, 1, 12));

    // Prior year - contributes to balanceAtEndOfYear/balanceAtEndOfMonth carry-forward, but
    // must not leak into this year's yearlyIncome/monthlyCashFlow figures.
    final Transaction txPriorYear =
        TransactionTestFactory.createTransaction(account, incomeCategory, TransactionType.INCOME,
            BigDecimal.valueOf(300), TransactionStatus.ACTIVE, LocalDate.of(targetYear - 1, 12, 31));

    transactionRepository.saveAll(
        List.of(openingTx, txJanIncome, txJanSecondIncome, txFebIncome, txJanExpense, txDeleted, txPriorYear));

    // Expected Math:
    // Opening balance: 5000. Prior year (2025) closing balance: 5000 + 300 = 5300.
    // Jan 2026: Income = 1230, Expense = 200, Net = 1030. Balance = 5300 + 1030 = 6330.
    // Feb 2026: Income = 500, Expense = 0, Net = 500. Balance = 6330 + 500 = 6830.
    // Total 2026: Income = 1730, Expense = 200, Net = 1530. balanceAtEndOfYear = 6830.

    // Act
    final Response response = given()
        .header("Authorization", "Bearer " + userSid)
        .pathParam("accountSid", account.getSid())
        .queryParam("year", targetYear)
        .contentType(ContentType.JSON)
        .when()
        .get("/analytics/{accountSid}/cash-flow");

    // Assert
    response.then()
        .log().ifValidationFails()
        .statusCode(200)
        .body("year", is(targetYear))
        .body("yearlyIncome", is(1730.0f))
        .body("yearlyExpense", is(200.0f))
        .body("yearlyTransfersIn", is(0))
        .body("yearlyTransfersOut", is(0))
        .body("yearlyNetIncomeExpense", is(1530.0f))
        .body("balanceAtEndOfYear", is(6830.0f))

        .body("monthlyCashFlow.size()", is(12))

        .body("monthlyCashFlow[0].month", is(1))
        .body("monthlyCashFlow[0].income", is(1230.0f))
        .body("monthlyCashFlow[0].expense", is(200.0f))
        .body("monthlyCashFlow[0].monthlyNetIncomeExpense", is(1030.0f))
        .body("monthlyCashFlow[0].balanceAtEndOfMonth", is(6330.0f))

        .body("monthlyCashFlow[1].month", is(2))
        .body("monthlyCashFlow[1].income", is(500.0f))
        .body("monthlyCashFlow[1].expense", is(0))
        .body("monthlyCashFlow[1].monthlyNetIncomeExpense", is(500.0f))
        .body("monthlyCashFlow[1].balanceAtEndOfMonth", is(6830.0f))

        .body("monthlyCashFlow[2].month", is(3))
        .body("monthlyCashFlow[2].income", is(0))
        .body("monthlyCashFlow[2].expense", is(0))
        .body("monthlyCashFlow[2].monthlyNetIncomeExpense", is(0))
        .body("monthlyCashFlow[2].balanceAtEndOfMonth", is(6830.0f))

        .body("monthlyCashFlow[3].month", is(4))
        .body("monthlyCashFlow[3].income", is(0))
        .body("monthlyCashFlow[3].expense", is(0))
        .body("monthlyCashFlow[3].monthlyNetIncomeExpense", is(0));
  }

  @Test
  void shouldReturnEmptyReportForBrandNewAccountWithNoActivityYet() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final BigDecimal openingBalance = BigDecimal.valueOf(1000);
    final Account account =
        AccountTestFactory.createAccountWithOwner(userSid, "Fresh Account", openingBalance);
    accountRepository.save(account);

    final Category openingBalanceCategory = fetchOpeningBalanceCategory();
    final int targetYear = Year.now().getValue();

    final Transaction openingTx = TransactionTestFactory.createOpeningBalanceTransaction(
        account, openingBalanceCategory, openingBalance, LocalDate.of(targetYear, 1, 1));
    transactionRepository.save(openingTx);

    // Act
    final Response response = given()
        .header("Authorization", "Bearer " + userSid)
        .pathParam("accountSid", account.getSid())
        .queryParam("year", targetYear)
        .contentType(ContentType.JSON)
        .when()
        .get("/analytics/{accountSid}/cash-flow");

    // Assert
    response.then()
        .log().ifValidationFails()
        .statusCode(200)
        .body("year", is(targetYear))
        .body("yearlyIncome", is(0))
        .body("yearlyExpense", is(0))
        .body("yearlyNetIncomeExpense", is(0))
        .body("balanceAtEndOfYear", is(1000.0f))
        .body("yearsWithCashFlows.size()", is(0))
        .body("monthlyCashFlow.size()", is(12))
        .body("monthlyCashFlow[0].balanceAtEndOfMonth", is(1000.0f))
        .body("monthlyCashFlow[11].balanceAtEndOfMonth", is(1000.0f));
  }

  @Test
  void shouldRejectRequestForYearBeforeAccountExisted() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final BigDecimal openingBalance = BigDecimal.valueOf(1000);
    final Account account =
        AccountTestFactory.createAccountWithOwner(userSid, "Main Checking", openingBalance);
    accountRepository.save(account);

    final Category openingBalanceCategory = fetchOpeningBalanceCategory();
    final int accountOpeningYear = 2024;

    final Transaction openingTx = TransactionTestFactory.createOpeningBalanceTransaction(
        account, openingBalanceCategory, openingBalance, LocalDate.of(accountOpeningYear, 1, 1));
    transactionRepository.save(openingTx);

    // Act
    final Response response = given()
        .header("Authorization", "Bearer " + userSid)
        .pathParam("accountSid", account.getSid())
        .queryParam("year", accountOpeningYear - 1)
        .contentType(ContentType.JSON)
        .when()
        .get("/analytics/{accountSid}/cash-flow");

    // Assert
    response.then()
        .log().ifValidationFails()
        .statusCode(400);
  }

  @Test
  void shouldRejectRequestForFutureYear() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account =
        AccountTestFactory.createAccountWithOwner(userSid, "Main Checking", BigDecimal.valueOf(1000));
    accountRepository.save(account);

    // Act
    final Response response = given()
        .header("Authorization", "Bearer " + userSid)
        .pathParam("accountSid", account.getSid())
        .queryParam("year", Year.now().getValue() + 1)
        .contentType(ContentType.JSON)
        .when()
        .get("/analytics/{accountSid}/cash-flow");

    // Assert
    response.then()
        .log().ifValidationFails()
        .statusCode(400);
  }

  @Test
  void shouldIncludeTransfersInBalanceButKeepThemOutOfIncomeAndExpense() {
    // Arrange - a transfer in and a transfer out during the target year. The balance must
    // reflect both, exactly like the real account balance would, while income/expense stay
    // untouched.
    final UUID userSid = UUID.randomUUID();
    final BigDecimal openingBalance = BigDecimal.valueOf(1000);
    final Account account =
        AccountTestFactory.createAccountWithOwner(userSid, "Main Checking", openingBalance);
    accountRepository.save(account);

    final Category openingBalanceCategory = fetchOpeningBalanceCategory();
    final Category transferInCategory = categoryService.fetchInternalCategory(SystemCategory.TRANSFER_IN.getSid());
    final Category transferOutCategory = categoryService.fetchInternalCategory(SystemCategory.TRANSFER_OUT.getSid());

    final int targetYear = 2026;

    final Transaction openingTx = TransactionTestFactory.createOpeningBalanceTransaction(
        account, openingBalanceCategory, openingBalance, LocalDate.of(targetYear, 1, 1));

    final Transaction txJanTransferIn =
        TransactionTestFactory.createTransaction(account, transferInCategory, TransactionType.TRANSFER_IN,
            BigDecimal.valueOf(500), TransactionStatus.ACTIVE, LocalDate.of(targetYear, 1, 20));
    final Transaction txMarTransferOut =
        TransactionTestFactory.createTransaction(account, transferOutCategory, TransactionType.TRANSFER_OUT,
            BigDecimal.valueOf(200), TransactionStatus.ACTIVE, LocalDate.of(targetYear, 3, 5));

    transactionRepository.saveAll(List.of(openingTx, txJanTransferIn, txMarTransferOut));

    // Opening balance: 1000. Jan: +500 transfer in -> 1500. Feb: flat -> 1500.
    // Mar: -200 transfer out -> 1300. balanceAtEndOfYear = 1300.

    // Act
    final Response response = given()
        .header("Authorization", "Bearer " + userSid)
        .pathParam("accountSid", account.getSid())
        .queryParam("year", targetYear)
        .contentType(ContentType.JSON)
        .when()
        .get("/analytics/{accountSid}/cash-flow");

    // Assert
    response.then()
        .log().ifValidationFails()
        .statusCode(200)
        .body("yearlyIncome", is(0))
        .body("yearlyExpense", is(0))
        .body("yearlyNetIncomeExpense", is(0))
        .body("yearlyTransfersIn", is(500.0f))
        .body("yearlyTransfersOut", is(200.0f))
        .body("balanceAtEndOfYear", is(1300.0f))

        .body("monthlyCashFlow[0].transfersIn", is(500.0f))
        .body("monthlyCashFlow[0].transfersOut", is(0))
        .body("monthlyCashFlow[0].income", is(0))
        .body("monthlyCashFlow[0].balanceAtEndOfMonth", is(1500.0f))

        .body("monthlyCashFlow[1].balanceAtEndOfMonth", is(1500.0f))

        .body("monthlyCashFlow[2].transfersOut", is(200.0f))
        .body("monthlyCashFlow[2].balanceAtEndOfMonth", is(1300.0f));
  }

  @Test
  void shouldReflectRealTransferBetweenTwoOwnedAccountsInBothCashFlowReports() {
    // Arrange - a real Transfer (via TransferService) between two of the same user's
    // accounts. Both accounts' cash flow reports must reflect the movement in their balance.
    final UUID userSid = UUID.randomUUID();
    final BigDecimal sourceOpeningBalance = BigDecimal.valueOf(2000);
    final BigDecimal destinationOpeningBalance = BigDecimal.valueOf(500);

    final Account sourceAccount =
        AccountTestFactory.createAccountWithOwner(userSid, "Checking", sourceOpeningBalance);
    final Account destinationAccount =
        AccountTestFactory.createAccountWithOwner(userSid, "Savings", destinationOpeningBalance);
    accountRepository.save(sourceAccount);
    accountRepository.save(destinationAccount);

    final Category openingBalanceCategory = fetchOpeningBalanceCategory();
    final int targetYear = 2026;

    final Transaction sourceOpeningTx = TransactionTestFactory.createOpeningBalanceTransaction(
        sourceAccount, openingBalanceCategory, sourceOpeningBalance, LocalDate.of(targetYear, 1, 1));
    final Transaction destinationOpeningTx = TransactionTestFactory.createOpeningBalanceTransaction(
        destinationAccount, openingBalanceCategory, destinationOpeningBalance, LocalDate.of(targetYear, 1, 1));
    transactionRepository.saveAll(List.of(sourceOpeningTx, destinationOpeningTx));

    transferService.createTransfer(new CreateTransferCommand(
        userSid, sourceAccount.getSid(), destinationAccount.getSid(),
        BigDecimal.valueOf(300), "Move to savings", LocalDateTime.of(targetYear, 2, 1, 12, 0)));

    // Source: 2000 - 300 = 1700. Destination: 500 + 300 = 800.

    // Act
    final Response sourceResponse = given()
        .header("Authorization", "Bearer " + userSid)
        .pathParam("accountSid", sourceAccount.getSid())
        .queryParam("year", targetYear)
        .contentType(ContentType.JSON)
        .when()
        .get("/analytics/{accountSid}/cash-flow");

    final Response destinationResponse = given()
        .header("Authorization", "Bearer " + userSid)
        .pathParam("accountSid", destinationAccount.getSid())
        .queryParam("year", targetYear)
        .contentType(ContentType.JSON)
        .when()
        .get("/analytics/{accountSid}/cash-flow");

    // Assert
    sourceResponse.then()
        .log().ifValidationFails()
        .statusCode(200)
        .body("yearlyTransfersOut", is(300.0f))
        .body("yearlyTransfersIn", is(0))
        .body("balanceAtEndOfYear", is(1700.0f))
        .body("monthlyCashFlow[1].balanceAtEndOfMonth", is(1700.0f));

    destinationResponse.then()
        .log().ifValidationFails()
        .statusCode(200)
        .body("yearlyTransfersIn", is(300.0f))
        .body("yearlyTransfersOut", is(0))
        .body("balanceAtEndOfYear", is(800.0f))
        .body("monthlyCashFlow[1].balanceAtEndOfMonth", is(800.0f));
  }

  @Test
  void shouldCarryBalanceForwardThroughGapYearWithNoActivity() {
    // Arrange - opening balance + income in 2023, nothing in 2024, income in 2025.
    // Requesting 2025 must carry the 2023 closing balance forward through the 2024 gap.
    final UUID userSid = UUID.randomUUID();
    final BigDecimal openingBalance = BigDecimal.valueOf(1000);
    final Account account =
        AccountTestFactory.createAccountWithOwner(userSid, "Main Checking", openingBalance);
    accountRepository.save(account);

    final Category incomeCategory = fetchCategory(userSid, TransactionType.INCOME);
    final Category openingBalanceCategory = fetchOpeningBalanceCategory();

    final Transaction openingTx = TransactionTestFactory.createOpeningBalanceTransaction(
        account, openingBalanceCategory, openingBalance, LocalDate.of(2023, 1, 1));
    final Transaction tx2023Income =
        TransactionTestFactory.createTransaction(account, incomeCategory, TransactionType.INCOME,
            BigDecimal.valueOf(300), TransactionStatus.ACTIVE, LocalDate.of(2023, 6, 1));
    final Transaction tx2025Income =
        TransactionTestFactory.createTransaction(account, incomeCategory, TransactionType.INCOME,
            BigDecimal.valueOf(500), TransactionStatus.ACTIVE, LocalDate.of(2025, 1, 10));

    transactionRepository.saveAll(List.of(openingTx, tx2023Income, tx2025Income));

    // 2023 closing balance = 1000 + 300 = 1300. 2024 has no activity.
    // January 2025 closing balance = 1300 + 500 = 1800.

    // Act
    final Response response = given()
        .header("Authorization", "Bearer " + userSid)
        .pathParam("accountSid", account.getSid())
        .queryParam("year", 2025)
        .contentType(ContentType.JSON)
        .when()
        .get("/analytics/{accountSid}/cash-flow");

    // Assert
    response.then()
        .log().ifValidationFails()
        .statusCode(200)
        .body("balanceAtEndOfYear", is(1800.0f))
        .body("monthlyCashFlow[0].balanceAtEndOfMonth", is(1800.0f));
  }

  private Category fetchCategory(final UUID userSid, final TransactionType type) {
    return categoryRepository.findAll(new FindAllCategoriesCommand(userSid, null, null, null, null)).stream()
        .filter(c -> c.getTransactionType() == type)
        .findFirst().get();
  }

  private Category fetchOpeningBalanceCategory() {
    return categoryService.fetchInternalCategory(SystemCategory.INITIAL_BALANCE.getSid());
  }

}
