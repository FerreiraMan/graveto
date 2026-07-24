package me.ferreira.graveto.moneytracker.transactions.recurringtransfer;

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
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.RecurringTransferRepository;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransfer.CreateRecurringTransferRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CreateRecurringTransferIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private RecurringTransferRepository recurringTransferRepository;
  @Autowired
  private AccountRepository accountRepository;

  @Test
  void shouldCreateRecurringTransfer() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(userSid, "Santander");
    final Account destinationAccount = setupAccount(userSid, "BCP");
    final LocalDate startDate = LocalDate.now().plusMonths(1).withDayOfMonth(15);

    final CreateRecurringTransferRequestDto request = new CreateRecurringTransferRequestDto(
        sourceAccount.getSid(), destinationAccount.getSid(), "Home Insurance", new BigDecimal("50.00"),
        Frequency.MONTHLY, 15, null, true, startDate, null);

    // Act
    final String rtSid = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/recurring-transfers")
        .then()
        .statusCode(201)
        .header("Location", notNullValue())
        .body("sid", notNullValue())
        .body("sourceAccount.name", equalTo("Santander"))
        .body("destinationAccount.name", equalTo("BCP"))
        .body("frequency", equalTo("MONTHLY"))
        .body("status", equalTo("ACTIVE"))
        .body("nextExecutionDate", equalTo(startDate.toString()))
        .extract()
        .path("sid");

    // Assert
    final List<RecurringTransfer> persistedRt =
        recurringTransferRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
            RecurringOperationStatus.ACTIVE, startDate);

    assertThat(persistedRt).hasSize(1);
    assertThat(persistedRt.getFirst().getSid()).isEqualTo(UUID.fromString(rtSid));
    assertThat(persistedRt.getFirst().getSourceAccount().getSid()).isEqualTo(sourceAccount.getSid());
    assertThat(persistedRt.getFirst().getDestinationAccount().getSid()).isEqualTo(destinationAccount.getSid());
    assertThat(persistedRt.getFirst().getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    assertThat(persistedRt.getFirst().getFrequency()).isEqualTo(Frequency.MONTHLY);
    assertThat(persistedRt.getFirst().getDayOfTheMonth()).isEqualTo(15);
    assertThat(persistedRt.getFirst().getAdjustToBusinessDay()).isTrue();
    assertThat(persistedRt.getFirst().getStartDate()).isEqualTo(startDate);
    assertThat(persistedRt.getFirst().getEndDate()).isNull();
  }

  @Test
  void shouldCreateRecurringTransferWithResolvedStartDate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(userSid, "Santander");
    final Account destinationAccount = setupAccount(userSid, "BCP");

    final CreateRecurringTransferRequestDto request = new CreateRecurringTransferRequestDto(
        sourceAccount.getSid(), destinationAccount.getSid(), "Internet", new BigDecimal("35.00"), Frequency.MONTHLY, 15,
        null, true, null, null);

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/recurring-transfers")
        .then()
        .statusCode(201)
        .body("nextExecutionDate", notNullValue())
        .body("status", equalTo("ACTIVE"));
  }

  @Test
  void shouldReturnBadRequestWhenMonthlyAndDayOfMonthMissing() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(userSid, "Santander");
    final Account destinationAccount = setupAccount(userSid, "BCP");

    final CreateRecurringTransferRequestDto request = new CreateRecurringTransferRequestDto(
        sourceAccount.getSid(), destinationAccount.getSid(), "Test", new BigDecimal("10.00"), Frequency.MONTHLY, null,
        null, true, null, null);

    // Act & Assert
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/recurring-transfers")
        .then()
        .statusCode(400);
  }

  @Test
  void shouldReturnForbiddenWhenUserIsNotMemberOfAccount() {
    // Arrange
    final UUID ownerSid = UUID.randomUUID();
    final UUID otherUserSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(ownerSid, "Santander");
    final Account destinationAccount = setupAccount(ownerSid, "BCP");

    final CreateRecurringTransferRequestDto request = new CreateRecurringTransferRequestDto(
        sourceAccount.getSid(), destinationAccount.getSid(), "Test", new BigDecimal("10.00"), Frequency.MONTHLY, 15,
        null, true,
        LocalDate.now().plusMonths(1), null);

    // Act & Assert
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + otherUserSid)
        .body(request)
        .when()
        .post("/recurring-transfers")
        .then()
        .statusCode(403);
  }

  private Account setupAccount(final UUID userSid, final String name) {
    final Account account = Account.create(BigDecimal.ZERO, Currency.EUR, name);
    account.addMembership(AccountMembership.create(userSid, MembershipRole.OWNER));
    return accountRepository.save(account);
  }

}
