package me.ferreira.graveto.moneytracker.analytics;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FetchCategorySpendingReportIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private TransactionRepository transactionRepository;
  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private CategoryRepository categoryRepository;

  @Test
  void shouldFetchCategorySpendingReportWithAccurateInfiniteDepthAggregations() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account =
        AccountTestFactory.createAccountWithOwner(userSid, "Main Checking", BigDecimal.valueOf(5000));
    accountRepository.save(account);

    final Category housing = categoryRepository.findByUserSidIsNull().stream()
        .filter(c -> c.getName().equals("HOUSING")).findFirst().orElseThrow();
    final Category utilities = categoryRepository.findByUserSidIsNull().stream()
        .filter(c -> c.getName().equals("UTILITIES")).findFirst().orElseThrow();
    final Category electricity = categoryRepository.findByUserSidIsNull().stream()
        .filter(c -> c.getName().equals("ELECTRICITY")).findFirst().orElseThrow();

    final int targetYear = 2026;

    // Level 3 tx
    final Transaction txJanElectricity =
        TransactionTestFactory.createTransaction(account, electricity, TransactionType.EXPENSE,
            BigDecimal.valueOf(50), TransactionStatus.ACTIVE, LocalDate.of(targetYear, 1, 10));

    // Level 2 tx
    final Transaction txJanUtilities =
        TransactionTestFactory.createTransaction(account, utilities, TransactionType.EXPENSE,
            BigDecimal.valueOf(20), TransactionStatus.ACTIVE, LocalDate.of(targetYear, 1, 15));

    // Level 1 tx
    final Transaction txFebHousing = TransactionTestFactory.createTransaction(account, housing, TransactionType.EXPENSE,
        BigDecimal.valueOf(100), TransactionStatus.ACTIVE, LocalDate.of(targetYear, 2, 5));

    // Deleted - should be ignored
    final Transaction txDeleted =
        TransactionTestFactory.createTransaction(account, electricity, TransactionType.EXPENSE,
            BigDecimal.valueOf(9999), TransactionStatus.DELETED, LocalDate.of(targetYear, 1, 12));

    // Wrong year - should be ignored
    final Transaction txWrongYear =
        TransactionTestFactory.createTransaction(account, utilities, TransactionType.EXPENSE,
            BigDecimal.valueOf(50000), TransactionStatus.ACTIVE, LocalDate.of(2025, 12, 31));

    transactionRepository.saveAll(List.of(txJanElectricity, txJanUtilities, txFebHousing, txDeleted, txWrongYear));

    // Expected Math for 2026:
    // Electricity (L3): Jan = 50. Total = 50.
    // Utilities (L2): Jan = 20 + 50 = 70. Total = 70.
    // Housing (L1): Jan = 70. Feb = 100. Total = 170.

    // Act
    final Response response = given()
        .header("Authorization", "Bearer " + userSid)
        .pathParam("accountSid", account.getSid())
        .queryParam("year", targetYear)
        .contentType(ContentType.JSON)
        .when()
        .get("/analytics/{accountSid}/category-spending");

    // Assert
    response.then()
        .log().ifValidationFails()
        .statusCode(200)
        .body("year", is(targetYear))
        .body("categories.size()", is(1))

        // Level 1 (Housing)
        .body("categories[0].categorySid", is(housing.getSid().toString()))
        .body("categories[0].yearlyTotal", is(170.0f))
        .body("categories[0].monthlyTotals.1", is(70.0f))
        .body("categories[0].monthlyTotals.2", is(100.0f))
        .body("categories[0].monthlyTotals.3", is(0))
        .body("categories[0].childCategories.size()", is(1))

        // Level 2 (Utilities)
        .body("categories[0].childCategories[0].categorySid", is(utilities.getSid().toString()))
        .body("categories[0].childCategories[0].yearlyTotal", is(70.0f))
        .body("categories[0].childCategories[0].monthlyTotals.1", is(70.0f))
        .body("categories[0].childCategories[0].monthlyTotals.2", is(0))
        .body("categories[0].childCategories[0].childCategories.size()", is(1))

        // Level 3 (Electricity)
        .body("categories[0].childCategories[0].childCategories[0].categorySid", is(electricity.getSid().toString()))
        .body("categories[0].childCategories[0].childCategories[0].yearlyTotal", is(50.0f))
        .body("categories[0].childCategories[0].childCategories[0].monthlyTotals.1", is(50.0f))
        .body("categories[0].childCategories[0].childCategories[0].childCategories.size()", is(0));
  }

}
