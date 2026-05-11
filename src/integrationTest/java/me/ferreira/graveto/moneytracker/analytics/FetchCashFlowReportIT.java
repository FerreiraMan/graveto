package me.ferreira.graveto.moneytracker.analytics;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import me.ferreira.graveto.moneytracker.utils.TransactionTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FetchCashFlowReportIT extends MoneyTrackerBaseIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldFetchCashFlowReportWithAccurateAggregations() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final Account account = AccountTestFactory.createAccountWithOwner(userSid, "Main Checking", BigDecimal.valueOf(5000));
        accountRepository.save(account);

        final Category incomeCategory = categoryRepository.findByUserSidIsNull().stream()
                .filter(c -> c.getTransactionType() == TransactionType.INCOME)
                .findFirst().get();
        final Category expenseCategory = categoryRepository.findByUserSidIsNull().stream()
                .filter(c -> c.getTransactionType() == TransactionType.EXPENSE)
                .findFirst().get();

        final int targetYear = 2026;

        // Income
        final Transaction txJanIncome = TransactionTestFactory.createTransaction(account, incomeCategory, TransactionType.INCOME,
                BigDecimal.valueOf(1000), TransactionStatus.ACTIVE, LocalDate.of(targetYear, 1, 10));
        final Transaction txJanSecondIncome = TransactionTestFactory.createTransaction(account, incomeCategory, TransactionType.INCOME,
                BigDecimal.valueOf(230), TransactionStatus.ACTIVE, LocalDate.of(targetYear, 1, 10));
        final Transaction txFebIncome = TransactionTestFactory.createTransaction(account, incomeCategory, TransactionType.INCOME,
                BigDecimal.valueOf(500), TransactionStatus.ACTIVE, LocalDate.of(targetYear, 2, 10));

        // Expense
        final Transaction txJanExpense = TransactionTestFactory.createTransaction(account, expenseCategory, TransactionType.EXPENSE,
                BigDecimal.valueOf(200), TransactionStatus.ACTIVE, LocalDate.of(targetYear, 1, 15));

        // Deleted - should be ignored
        final Transaction txDeleted = TransactionTestFactory.createTransaction(account, expenseCategory, TransactionType.EXPENSE,
                BigDecimal.valueOf(9999), TransactionStatus.DELETED, LocalDate.of(targetYear, 1, 12));

        // Wrong year - should be ignored
        final Transaction txWrongYear = TransactionTestFactory.createTransaction(account, incomeCategory, TransactionType.INCOME,
                BigDecimal.valueOf(50000), TransactionStatus.ACTIVE, LocalDate.of(2025, 12, 31));

        transactionRepository.saveAll(List.of(txJanIncome, txJanSecondIncome, txFebIncome, txJanExpense, txDeleted, txWrongYear));

        // Expected Math for 2026:
        // Jan: Income = 1230, Expense = 200, Net = 1030
        // Feb: Income = 500, Expense = 0, Net = 500
        // Total: Income = 1730, Expense = 200, Net = 1530

        // Act
        final Response response = given().
                header("Authorization", "Bearer " + userSid).
                pathParam("accountSid", account.getSid()).
                queryParam("year", targetYear).
                contentType(ContentType.JSON).
                when().
                get("/analytics/{accountSid}/cash-flow");

        // Assert
        response.then().
                log().ifValidationFails().
                statusCode(200).
                body("year", is(targetYear)).
                body("yearlyIncome", is(1730.0f)).
                body("yearlyExpense", is(200.0f)).
                body("yearlyNetFlow", is(1530.0f)).

                body("monthlyCashFlow.size()", is(12)).

                body("monthlyCashFlow[0].month", is(1)).
                body("monthlyCashFlow[0].income", is(1230.0f)).
                body("monthlyCashFlow[0].expense", is(200.0f)).
                body("monthlyCashFlow[0].netFlow", is(1030.0f)).

                body("monthlyCashFlow[1].month", is(2)).
                body("monthlyCashFlow[1].income", is(500.0f)).
                body("monthlyCashFlow[1].expense", is(0)).
                body("monthlyCashFlow[1].netFlow", is(500.0f)).

                body("monthlyCashFlow[2].month", is(3)).
                body("monthlyCashFlow[2].income", is(0)).
                body("monthlyCashFlow[2].expense", is(0)).
                body("monthlyCashFlow[2].netFlow", is(0)).

                body("monthlyCashFlow[3].month", is(4)).
                body("monthlyCashFlow[3].income", is(0)).
                body("monthlyCashFlow[3].expense", is(0)).
                body("monthlyCashFlow[3].netFlow", is(0));
    }

}
