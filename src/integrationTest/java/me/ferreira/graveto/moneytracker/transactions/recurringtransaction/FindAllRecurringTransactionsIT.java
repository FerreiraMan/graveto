package me.ferreira.graveto.moneytracker.transactions.recurringtransaction;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;

import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransaction.CreateRecurringTransactionRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FindAllRecurringTransactionsIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private CategoryRepository categoryRepository;

  @Test
  void shouldReturnAllRecurringTransactionsForUser() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);
    createRecurringTransaction(userSid, account, "Insurance", new BigDecimal("50"), LocalDate.now().plusDays(10));
    createRecurringTransaction(userSid, account, "Rent", new BigDecimal("800"), LocalDate.now().plusDays(20));

    // Act & Assert
    given()
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/recurring-transactions")
        .then()
        .statusCode(200)
        .body("$", hasSize(2));
  }

  @Test
  void shouldReturnEmptyListWhenUserHasNoRecurringTransactions() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    setupAccount(userSid);

    // Act & Assert
    given()
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/recurring-transactions")
        .then()
        .statusCode(200)
        .body("$", hasSize(0));
  }

  @Test
  void shouldFilterByStatus() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);

    createRecurringTransaction(userSid, account, "Insurance", new BigDecimal("50"), LocalDate.now().plusDays(10));
    final String pausedSid =
        createRecurringTransaction(userSid, account, "Gym", new BigDecimal("30"), LocalDate.now().plusDays(15));

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body("""
            {"accountSid": "%s", "status": "PAUSED"}
            """.formatted(account.getSid()))
        .when()
        .patch("/recurring-transactions/" + pausedSid)
        .then()
        .statusCode(200);

    // Act
    given()
        .header("Authorization", "Bearer " + userSid)
        .queryParam("status", "ACTIVE")
        .when()
        .get("/recurring-transactions")
        .then()
        .statusCode(200)
        .body("$", hasSize(1));
  }

  @Test
  void shouldFilterByAccountSid() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account1 = setupAccount(userSid);
    final Account account2 = setupAccountWithInstitution(userSid, "Revolut");

    createRecurringTransaction(userSid, account1, "Insurance", new BigDecimal("50"), LocalDate.now().plusDays(10));
    createRecurringTransaction(userSid, account2, "Subscription", new BigDecimal("15"), LocalDate.now().plusDays(5));

    // Act
    given()
        .header("Authorization", "Bearer " + userSid)
        .queryParam("accountSid", account1.getSid().toString())
        .when()
        .get("/recurring-transactions")
        .then()
        .statusCode(200)
        .body("$", hasSize(1));
  }

  @Test
  void shouldNotReturnRecurringTransactionsFromOtherUsers() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID otherUserSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);
    final Account otherAccount = setupAccount(otherUserSid);

    createRecurringTransaction(userSid, account, "Insurance", new BigDecimal("50"), LocalDate.now().plusDays(10));
    createRecurringTransaction(otherUserSid, otherAccount, "Other Insurance", new BigDecimal("60"),
        LocalDate.now().plusDays(10));

    // Act
    given()
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/recurring-transactions")
        .then()
        .statusCode(200)
        .body("$", hasSize(1));
  }

  @Test
  void shouldReturnSortedByEarliestExecutionDate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);
    createRecurringTransaction(userSid, account, "Later", new BigDecimal("50"), LocalDate.now().plusDays(30));
    createRecurringTransaction(userSid, account, "Sooner", new BigDecimal("50"), LocalDate.now().plusDays(5));

    // Act
    final List<String> descriptions = given()
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/recurring-transactions")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath().getList("description", String.class);

    // Assert
    assertThat(descriptions).containsExactly("Sooner", "Later");
  }

  private String createRecurringTransaction(final UUID userSid, final Account account,
                                            final String description, final BigDecimal amount,
                                            final LocalDate startDate) {

    final Category category = setupCategory();

    final CreateRecurringTransactionRequestDto request = new CreateRecurringTransactionRequestDto(
        account.getSid(), category.getSid(), description, amount,
        TransactionType.EXPENSE, Frequency.MONTHLY, 15, null, true, startDate, null);

    return given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/recurring-transactions")
        .then()
        .statusCode(201)
        .extract()
        .path("sid");
  }

  private Account setupAccount(final UUID userSid) {
    return setupAccountWithInstitution(userSid, "Santander");
  }

  private Account setupAccountWithInstitution(final UUID userSid, final String institution) {
    final Account account = Account.create(BigDecimal.ZERO, Currency.EUR, institution);
    account.addMembership(AccountMembership.create(userSid, MembershipRole.OWNER));
    return accountRepository.save(account);
  }

  private Category setupCategory() {
    final List<Category> categories = categoryRepository.findAll();
    return categories.stream()
        .filter(c -> c.getTransactionType() == TransactionType.EXPENSE)
        .findFirst()
        .orElseThrow();
  }

}
