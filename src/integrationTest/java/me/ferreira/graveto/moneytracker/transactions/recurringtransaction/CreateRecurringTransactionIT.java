package me.ferreira.graveto.moneytracker.transactions.recurringtransaction;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction.RecurringTransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransaction.CreateRecurringTransactionRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CreateRecurringTransactionIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private RecurringTransactionRepository recurringTransactionRepository;
  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private CategoryRepository categoryRepository;

  @Test
  void shouldCreateRecurringTransaction() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);
    final Category category = setupCategory();
    final LocalDate startDate = LocalDate.now().plusMonths(1).withDayOfMonth(15);

    final CreateRecurringTransactionRequestDto request = new CreateRecurringTransactionRequestDto(
        account.getSid(), category.getSid(), "Home Insurance", new BigDecimal("50.00"),
        TransactionType.EXPENSE, Frequency.MONTHLY, 15, null, true, startDate, null);

    // Act
    final String rtSid = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/recurring-transactions")
        .then()
        .statusCode(201)
        .header("Location", notNullValue())
        .body("sid", notNullValue())
        .body("account.name", equalTo(account.getInstitution()))
        .body("frequency", equalTo("MONTHLY"))
        .body("status", equalTo("ACTIVE"))
        .body("nextExecutionDate", equalTo(startDate.toString()))
        .extract()
        .path("sid");

    // Assert
    final List<RecurringTransaction> persistedRt =
        recurringTransactionRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
            RecurringOperationStatus.ACTIVE, startDate);

    assertThat(persistedRt).hasSize(1);
    assertThat(persistedRt.getFirst().getSid()).isEqualTo(UUID.fromString(rtSid));
    assertThat(persistedRt.getFirst().getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    assertThat(persistedRt.getFirst().getFrequency()).isEqualTo(Frequency.MONTHLY);
    assertThat(persistedRt.getFirst().getDayOfTheMonth()).isEqualTo(15);
    assertThat(persistedRt.getFirst().getAdjustToBusinessDay()).isTrue();
    assertThat(persistedRt.getFirst().getStartDate()).isEqualTo(startDate);
    assertThat(persistedRt.getFirst().getEndDate()).isNull();
  }

  @Test
  void shouldCreateRecurringTransactionWithResolvedStartDate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);
    final Category category = setupCategory();

    final CreateRecurringTransactionRequestDto request = new CreateRecurringTransactionRequestDto(
        account.getSid(), category.getSid(), "Internet", new BigDecimal("35.00"),
        TransactionType.EXPENSE, Frequency.MONTHLY, 15, null, true, null, null);

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/recurring-transactions")
        .then()
        .statusCode(201)
        .body("nextExecutionDate", notNullValue())
        .body("status", equalTo("ACTIVE"));
  }

  @Test
  void shouldReturnBadRequestWhenCategoryTypeDoesNotMatch() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);
    final Category category = setupCategory(); // EXPENSE category

    final CreateRecurringTransactionRequestDto request = new CreateRecurringTransactionRequestDto(
        account.getSid(), category.getSid(), "Salary", new BigDecimal("3000.00"),
        TransactionType.INCOME, Frequency.MONTHLY, 25, null, true,
        LocalDate.now().plusMonths(1), null);

    // Act & Assert
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/recurring-transactions")
        .then()
        .statusCode(400);
  }

  @Test
  void shouldReturnBadRequestWhenMonthlyAndDayOfMonthMissing() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);
    final Category category = setupCategory();

    final CreateRecurringTransactionRequestDto request = new CreateRecurringTransactionRequestDto(
        account.getSid(), category.getSid(), "Test", new BigDecimal("10.00"),
        TransactionType.EXPENSE, Frequency.MONTHLY, null, null, true, null, null);

    // Act & Assert
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/recurring-transactions")
        .then()
        .statusCode(400);
  }

  @Test
  void shouldReturnForbiddenWhenUserIsNotMemberOfAccount() {
    // Arrange
    final UUID ownerSid = UUID.randomUUID();
    final UUID otherUserSid = UUID.randomUUID();
    final Account account = setupAccount(ownerSid);
    final Category category = setupCategory();

    final CreateRecurringTransactionRequestDto request = new CreateRecurringTransactionRequestDto(
        account.getSid(), category.getSid(), "Test", new BigDecimal("10.00"),
        TransactionType.EXPENSE, Frequency.MONTHLY, 15, null, true,
        LocalDate.now().plusMonths(1), null);

    // Act & Assert
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + otherUserSid)
        .body(request)
        .when()
        .post("/recurring-transactions")
        .then()
        .statusCode(403);
  }

  private Account setupAccount(final UUID userSid) {
    final Account account = Account.create(BigDecimal.ZERO, Currency.EUR, "Santander");
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
