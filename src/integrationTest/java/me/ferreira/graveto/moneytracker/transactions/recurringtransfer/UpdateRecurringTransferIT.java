package me.ferreira.graveto.moneytracker.transactions.recurringtransfer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.RecurringTransferRepository;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransfer.CreateRecurringTransferRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransfer.UpdateRecurringTransferRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UpdateRecurringTransferIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private RecurringTransferRepository recurringTransferRepository;
  @Autowired
  private AccountRepository accountRepository;

  @Test
  void shouldUpdateDescriptionAndAmount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(userSid, "Santander");
    final Account destinationAccount = setupAccount(userSid, "BCP");
    final String rtSid = createRecurringTransfer(userSid, sourceAccount, destinationAccount);

    final UpdateRecurringTransferRequestDto request = new UpdateRecurringTransferRequestDto(
        "Updated Insurance", new BigDecimal("75.00"),
        null, null, null, null, null, null, null);

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .patch("/recurring-transfers/" + rtSid)
        .then()
        .statusCode(200);

    // Assert
    final RecurringTransfer persisted = fetchPersistedRecord(rtSid);
    assertThat(persisted.getDescription()).isEqualTo("Updated Insurance");
    assertThat(persisted.getAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
    assertThat(persisted.getFrequency()).isEqualTo(Frequency.MONTHLY);
    assertThat(persisted.getStatus()).isEqualTo(RecurringOperationStatus.ACTIVE);
  }

  @Test
  void shouldPauseAndPreserveNextExecutionDate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(userSid, "Santander");
    final Account destinationAccount = setupAccount(userSid, "BCP");
    final String rtSid = createRecurringTransfer(userSid, sourceAccount, destinationAccount);

    final RecurringTransfer beforePause = fetchPersistedRecord(rtSid);
    final LocalDate originalNextExecution = beforePause.getNextExecutionDate();

    final UpdateRecurringTransferRequestDto request = new UpdateRecurringTransferRequestDto(
        null, null, null, null, null, null,
        RecurringOperationStatus.PAUSED, null, null);

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .patch("/recurring-transfers/" + rtSid)
        .then()
        .statusCode(200);

    // Assert
    final RecurringTransfer persisted = fetchPersistedRecord(rtSid);
    assertThat(persisted.getStatus()).isEqualTo(RecurringOperationStatus.PAUSED);
    assertThat(persisted.getNextExecutionDate()).isEqualTo(originalNextExecution);
  }

  @Test
  void shouldResumeAndRecalculateNextExecutionDate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(userSid, "Santander");
    final Account destinationAccount = setupAccount(userSid, "BCP");
    final String rtSid = createRecurringTransfer(userSid, sourceAccount, destinationAccount);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(new UpdateRecurringTransferRequestDto(
            null, null, null, null, null, null,
            RecurringOperationStatus.PAUSED, null, null))
        .when()
        .patch("/recurring-transfers/" + rtSid)
        .then()
        .statusCode(200);

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(new UpdateRecurringTransferRequestDto(
            null, null, null, null, null, null,
            RecurringOperationStatus.ACTIVE, null, null))
        .when()
        .patch("/recurring-transfers/" + rtSid)
        .then()
        .statusCode(200);

    // Assert
    final RecurringTransfer persisted = fetchPersistedRecord(rtSid);
    assertThat(persisted.getStatus()).isEqualTo(RecurringOperationStatus.ACTIVE);
    assertThat(persisted.getNextExecutionDate()).isNotNull();
  }

  @Test
  void shouldUpdateFrequencyAndRecalculateNextExecutionDate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(userSid, "Santander");
    final Account destinationAccount = setupAccount(userSid, "BCP");
    final String rtSid = createRecurringTransfer(userSid, sourceAccount, destinationAccount);

    final UpdateRecurringTransferRequestDto request = new UpdateRecurringTransferRequestDto(
        null, null, Frequency.WEEKLY, null, 3, null,
        null, null, null);

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .patch("/recurring-transfers/" + rtSid)
        .then()
        .statusCode(200);

    // Assert
    final RecurringTransfer persisted = fetchPersistedRecord(rtSid);
    assertThat(persisted.getFrequency()).isEqualTo(Frequency.WEEKLY);
    assertThat(persisted.getDayOfTheWeek()).isEqualTo(3);
    assertThat(persisted.getNextExecutionDate().getDayOfWeek().getValue()).isEqualTo(3);
  }

  @Test
  void shouldUseExplicitNextExecutionDateWhenProvided() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(userSid, "Santander");
    final Account destinationAccount = setupAccount(userSid, "BCP");
    final String rtSid = createRecurringTransfer(userSid, sourceAccount, destinationAccount);

    final LocalDate explicitDate = LocalDate.now().plusMonths(2).withDayOfMonth(1);

    final UpdateRecurringTransferRequestDto request = new UpdateRecurringTransferRequestDto(
        null, null, null, null, null, null,
        RecurringOperationStatus.ACTIVE, explicitDate, null);

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .patch("/recurring-transfers/" + rtSid)
        .then()
        .statusCode(200);

    // Assert
    final RecurringTransfer persisted = fetchPersistedRecord(rtSid);
    assertThat(persisted.getNextExecutionDate()).isEqualTo(explicitDate);
  }

  @Test
  void shouldReturnNotFoundWhenRecurringTransactionDoesNotExist() {
    // Arrange
    final UUID userSid = UUID.randomUUID();

    final UpdateRecurringTransferRequestDto request = new UpdateRecurringTransferRequestDto(
        null, new BigDecimal("100"), null, null, null, null,
        null, null, null);

    // Act & Assert
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .patch("/recurring-transfers/" + UUID.randomUUID())
        .then()
        .statusCode(404);
  }

  @Test
  void shouldReturnForbiddenWhenUserIsNotMemberOfAccount() {
    // Arrange
    final UUID ownerSid = UUID.randomUUID();
    final UUID otherUserSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(ownerSid, "Santander");
    final Account destinationAccount = setupAccount(ownerSid, "BCP");
    final String rtSid = createRecurringTransfer(ownerSid, sourceAccount, destinationAccount);

    final UpdateRecurringTransferRequestDto request = new UpdateRecurringTransferRequestDto(
        "Hacked", null, null, null, null, null,
        null, null, null);

    // Act & Assert
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + otherUserSid)
        .body(request)
        .when()
        .patch("/recurring-transfers/" + rtSid)
        .then()
        .statusCode(403);
  }

  private RecurringTransfer fetchPersistedRecord(final String rtSid) {
    return recurringTransferRepository.findBySid(UUID.fromString(rtSid))
        .orElseThrow();
  }

  private String createRecurringTransfer(final UUID userSid, final Account sourceAccount,
                                         final Account destinationAccount) {

    final LocalDate startDate = LocalDate.now().plusMonths(1).withDayOfMonth(15);

    final CreateRecurringTransferRequestDto request = new CreateRecurringTransferRequestDto(
        sourceAccount.getSid(), destinationAccount.getSid(), "Home Insurance", new BigDecimal("50.00"),
        Frequency.MONTHLY, 15, null, true, startDate, null);

    return given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/recurring-transfers")
        .then()
        .statusCode(201)
        .extract()
        .path("sid");
  }

  private Account setupAccount(final UUID userSid, final String name) {
    final Account account = Account.create(BigDecimal.ZERO, Currency.EUR, name);
    account.addMembership(AccountMembership.create(userSid, MembershipRole.OWNER));
    return accountRepository.save(account);
  }

}
