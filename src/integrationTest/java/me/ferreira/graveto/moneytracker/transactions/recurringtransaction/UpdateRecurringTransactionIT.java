package me.ferreira.graveto.moneytracker.transactions.recurringtransaction;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

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
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransaction.UpdateRecurringTransactionRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UpdateRecurringTransactionIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private RecurringTransactionRepository recurringTransactionRepository;
  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private CategoryRepository categoryRepository;

  @Test
  void shouldUpdateDescriptionAndAmount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);
    final String rtSid = createRecurringTransaction(userSid, account);

    final UpdateRecurringTransactionRequestDto request = new UpdateRecurringTransactionRequestDto(
        "Updated Insurance", new BigDecimal("75.00"),
        null, null, null, null, null, null, null);

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .patch("/recurring-transactions/" + rtSid)
        .then()
        .statusCode(200);

    // Assert
    final RecurringTransaction persisted = fetchPersistedRecord(rtSid);
    assertThat(persisted.getDescription()).isEqualTo("Updated Insurance");
    assertThat(persisted.getAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
    assertThat(persisted.getFrequency()).isEqualTo(Frequency.MONTHLY);
    assertThat(persisted.getStatus()).isEqualTo(RecurringOperationStatus.ACTIVE);
  }

  @Test
  void shouldPauseAndPreserveNextExecutionDate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);
    final String rtSid = createRecurringTransaction(userSid, account);

    final RecurringTransaction beforePause = fetchPersistedRecord(rtSid);
    final LocalDate originalNextExecution = beforePause.getNextExecutionDate();

    final UpdateRecurringTransactionRequestDto request = new UpdateRecurringTransactionRequestDto(
        null, null, null, null, null, null,
        RecurringOperationStatus.PAUSED, null, null);

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .patch("/recurring-transactions/" + rtSid)
        .then()
        .statusCode(200);

    // Assert
    final RecurringTransaction persisted = fetchPersistedRecord(rtSid);
    assertThat(persisted.getStatus()).isEqualTo(RecurringOperationStatus.PAUSED);
    assertThat(persisted.getNextExecutionDate()).isEqualTo(originalNextExecution);
  }

  @Test
  void shouldResumeAndRecalculateNextExecutionDate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);
    final String rtSid = createRecurringTransaction(userSid, account);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(new UpdateRecurringTransactionRequestDto(
            null, null, null, null, null, null,
            RecurringOperationStatus.PAUSED, null, null))
        .when()
        .patch("/recurring-transactions/" + rtSid)
        .then()
        .statusCode(200);

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(new UpdateRecurringTransactionRequestDto(
            null, null, null, null, null, null,
            RecurringOperationStatus.ACTIVE, null, null))
        .when()
        .patch("/recurring-transactions/" + rtSid)
        .then()
        .statusCode(200);

    // Assert
    final RecurringTransaction persisted = fetchPersistedRecord(rtSid);
    assertThat(persisted.getStatus()).isEqualTo(RecurringOperationStatus.ACTIVE);
    assertThat(persisted.getNextExecutionDate()).isNotNull();
  }

  @Test
  void shouldUpdateFrequencyAndRecalculateNextExecutionDate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);
    final String rtSid = createRecurringTransaction(userSid, account);

    final UpdateRecurringTransactionRequestDto request = new UpdateRecurringTransactionRequestDto(
        null, null, Frequency.WEEKLY, null, 3, null,
        null, null, null);

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .patch("/recurring-transactions/" + rtSid)
        .then()
        .statusCode(200);

    // Assert
    final RecurringTransaction persisted = fetchPersistedRecord(rtSid);
    assertThat(persisted.getFrequency()).isEqualTo(Frequency.WEEKLY);
    assertThat(persisted.getDayOfTheWeek()).isEqualTo(3);
    assertThat(persisted.getNextExecutionDate().getDayOfWeek().getValue()).isEqualTo(3);
  }

  @Test
  void shouldUseExplicitNextExecutionDateWhenProvided() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);
    final String rtSid = createRecurringTransaction(userSid, account);

    final LocalDate explicitDate = LocalDate.now().plusMonths(2).withDayOfMonth(1);

    final UpdateRecurringTransactionRequestDto request = new UpdateRecurringTransactionRequestDto(
        null, null, null, null, null, null,
        RecurringOperationStatus.ACTIVE, explicitDate, null);

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .patch("/recurring-transactions/" + rtSid)
        .then()
        .statusCode(200);

    // Assert
    final RecurringTransaction persisted = fetchPersistedRecord(rtSid);
    assertThat(persisted.getNextExecutionDate()).isEqualTo(explicitDate);
  }

  @Test
  void shouldReturnNotFoundWhenRecurringTransactionDoesNotExist() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);

    final UpdateRecurringTransactionRequestDto request = new UpdateRecurringTransactionRequestDto(
        null, new BigDecimal("100"), null, null, null, null,
        null, null, null);

    // Act & Assert
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .patch("/recurring-transactions/" + UUID.randomUUID())
        .then()
        .statusCode(404);
  }

  @Test
  void shouldReturnForbiddenWhenUserIsNotMemberOfAccount() {
    // Arrange
    final UUID ownerSid = UUID.randomUUID();
    final UUID otherUserSid = UUID.randomUUID();
    final Account account = setupAccount(ownerSid);
    final String rtSid = createRecurringTransaction(ownerSid, account);

    final UpdateRecurringTransactionRequestDto request = new UpdateRecurringTransactionRequestDto(
        "Hacked", null, null, null, null, null,
        null, null, null);

    // Act & Assert
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + otherUserSid)
        .body(request)
        .when()
        .patch("/recurring-transactions/" + rtSid)
        .then()
        .statusCode(403);
  }

  private RecurringTransaction fetchPersistedRecord(final String rtSid) {
    return recurringTransactionRepository.findBySid(UUID.fromString(rtSid))
        .orElseThrow();
  }

  private String createRecurringTransaction(final UUID userSid, final Account account) {
    final Category category = setupCategory();
    final LocalDate startDate = LocalDate.now().plusMonths(1).withDayOfMonth(15);

    final CreateRecurringTransactionRequestDto request = new CreateRecurringTransactionRequestDto(
        account.getSid(), category.getSid(), "Home Insurance", new BigDecimal("50.00"),
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
