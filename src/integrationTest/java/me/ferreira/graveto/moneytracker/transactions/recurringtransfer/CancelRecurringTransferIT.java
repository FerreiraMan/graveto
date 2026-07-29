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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CancelRecurringTransferIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private RecurringTransferRepository recurringTransferRepository;
  @Autowired
  private AccountRepository accountRepository;

  @Test
  void shouldCancelRecurringTransfer() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(userSid);
    final Account destinationAccount = setupAccount(userSid);
    final LocalDate endDate = LocalDate.of(2040, 1, 2);
    final String rtSid = createRecurringTransfer(userSid, sourceAccount, destinationAccount, endDate);

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .when()
        .delete("/recurring-transfers/" + rtSid)
        .then()
        .statusCode(200);

    // Assert
    final RecurringTransfer persisted = fetchPersistedRecord(rtSid);
    assertThat(persisted.getEndDate()).isNotEqualTo(endDate);
    assertThat(persisted.getStatus()).isEqualTo(RecurringOperationStatus.CANCELED);
  }

  @Test
  void shouldReturnNotFoundWhenRecurringTransferDoesNotExist() {
    // Arrange
    final UUID userSid = UUID.randomUUID();

    // Act & Assert
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .when()
        .delete("/recurring-transfers/" + UUID.randomUUID())
        .then()
        .statusCode(404);
  }

  @Test
  void shouldReturnForbiddenWhenUserIsNotMemberOfAccount() {
    // Arrange
    final UUID ownerSid = UUID.randomUUID();
    final UUID otherUserSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(ownerSid);
    final Account destinationAccount = setupAccount(ownerSid);
    final String rtSid =
        createRecurringTransfer(ownerSid, sourceAccount, destinationAccount, LocalDate.now().plusYears(2));

    // Act & Assert
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + otherUserSid)
        .when()
        .delete("/recurring-transfers/" + rtSid)
        .then()
        .statusCode(403);
  }

  private RecurringTransfer fetchPersistedRecord(final String rtSid) {
    return recurringTransferRepository.findBySid(UUID.fromString(rtSid))
        .orElseThrow();
  }

  private String createRecurringTransfer(final UUID userSid, final Account sourceAccount,
                                         final Account destinationAccount, final LocalDate endDate) {
    final LocalDate startDate = LocalDate.now().plusMonths(1).withDayOfMonth(15);

    final CreateRecurringTransferRequestDto request = new CreateRecurringTransferRequestDto(
        sourceAccount.getSid(), destinationAccount.getSid(), "Home Insurance", new BigDecimal("50.00"),
        Frequency.MONTHLY, 15, null,
        true, startDate, endDate);

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

  private Account setupAccount(final UUID userSid) {
    final Account account = Account.create(BigDecimal.ZERO, Currency.EUR, "Santander");
    account.addMembership(AccountMembership.create(userSid, MembershipRole.OWNER));
    return accountRepository.save(account);
  }

}
