package me.ferreira.graveto.moneytracker.transactions.recurringtransfer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

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
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransfer.CreateRecurringTransferRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FindAllRecurringTransfersIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private AccountRepository accountRepository;

  @Test
  void shouldReturnAllRecurringTransfersForUser() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(userSid, "Santander");
    final Account destinationAccount = setupAccount(userSid, "BCP");
    createRecurringTransfer(userSid, sourceAccount, destinationAccount, "Insurance", new BigDecimal("50"),
        LocalDate.now().plusDays(10));
    createRecurringTransfer(userSid, sourceAccount, destinationAccount, "Rent", new BigDecimal("800"),
        LocalDate.now().plusDays(20));

    // Act & Assert
    given()
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/recurring-transfers")
        .then()
        .statusCode(200)
        .body("$", hasSize(2));
  }

  @Test
  void shouldReturnEmptyListWhenUserHasNoRecurringTransfers() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    setupAccount(userSid, "BCP");

    // Act & Assert
    given()
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/recurring-transfers")
        .then()
        .statusCode(200)
        .body("$", hasSize(0));
  }

  @Test
  void shouldFilterByStatus() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(userSid, "Santander");
    final Account destinationAccount = setupAccount(userSid, "BCP");

    createRecurringTransfer(userSid, sourceAccount, destinationAccount, "Insurance", new BigDecimal("50"),
        LocalDate.now().plusDays(10));
    final String pausedSid =
        createRecurringTransfer(userSid, sourceAccount, destinationAccount, "Gym", new BigDecimal("30"),
            LocalDate.now().plusDays(15));

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body("""
            {"status": "PAUSED"}
            """)
        .when()
        .patch("/recurring-transfers/" + pausedSid)
        .then()
        .statusCode(200);

    // Act
    given()
        .header("Authorization", "Bearer " + userSid)
        .queryParam("status", "ACTIVE")
        .when()
        .get("/recurring-transfers")
        .then()
        .statusCode(200)
        .body("$", hasSize(1))
        .body("[0].sid", not(hasItem(pausedSid)));
  }

  @Test
  void shouldFilterBySourceAccountSid() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(userSid, "Santander");
    final Account secondSourceAccount = setupAccount(userSid, "BPI");
    final Account destinationAccount = setupAccount(userSid, "BCP");

    createRecurringTransfer(userSid, sourceAccount, destinationAccount, "Insurance", new BigDecimal("50"),
        LocalDate.now().plusDays(10));
    createRecurringTransfer(userSid, secondSourceAccount, destinationAccount, "Subscription", new BigDecimal("15"),
        LocalDate.now().plusDays(5));

    // Act
    given()
        .header("Authorization", "Bearer " + userSid)
        .queryParam("sourceAccountSid", sourceAccount.getSid().toString())
        .when()
        .get("/recurring-transfers")
        .then()
        .statusCode(200)
        .body("$", hasSize(1))
        .body("[0].sourceAccount.sid", not(hasItem(secondSourceAccount.getSid().toString())));
  }

  @Test
  void shouldFilterByDestinationAccountSid() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(userSid, "Santander");
    final Account destinationAccount = setupAccount(userSid, "BCP");
    final Account secondDestinationAccount = setupAccount(userSid, "BPI");

    createRecurringTransfer(userSid, sourceAccount, destinationAccount, "Insurance", new BigDecimal("50"),
        LocalDate.now().plusDays(10));
    createRecurringTransfer(userSid, sourceAccount, secondDestinationAccount, "Subscription", new BigDecimal("15"),
        LocalDate.now().plusDays(5));

    // Act
    given()
        .header("Authorization", "Bearer " + userSid)
        .queryParam("destinationAccountSid", destinationAccount.getSid().toString())
        .when()
        .get("/recurring-transfers")
        .then()
        .statusCode(200)
        .body("$", hasSize(1))
        .body("[0].destinationAccount.sid", not(hasItem(destinationAccount.getSid().toString())));
  }

  @Test
  void shouldNotReturnRecurringTransfersFromOtherUsers() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID otherUserSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(userSid, "Santander");
    final Account destinationAccount = setupAccount(userSid, "BCP");
    final Account otherSourceAccount = setupAccount(otherUserSid, "BPI");
    final Account otherDestinationAccount = setupAccount(otherUserSid, "BPI");

    createRecurringTransfer(userSid, sourceAccount, destinationAccount, "Insurance", new BigDecimal("50"),
        LocalDate.now().plusDays(10));
    createRecurringTransfer(otherUserSid, otherSourceAccount, otherDestinationAccount, "Other Insurance",
        new BigDecimal("60"),
        LocalDate.now().plusDays(10));

    // Act
    given()
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/recurring-transfers")
        .then()
        .statusCode(200)
        .body("$", hasSize(1))
        .body("[0].sourceAccount.sid", not(hasItem(sourceAccount.getSid().toString())));
  }

  @Test
  void shouldReturnSortedByEarliestExecutionDate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupAccount(userSid, "Santander");
    final Account destinationAccount = setupAccount(userSid, "BCP");
    createRecurringTransfer(userSid, sourceAccount, destinationAccount, "Later", new BigDecimal("50"),
        LocalDate.now().plusDays(30));
    createRecurringTransfer(userSid, sourceAccount, destinationAccount, "Sooner", new BigDecimal("50"),
        LocalDate.now().plusDays(5));

    // Act
    final List<String> descriptions = given()
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/recurring-transfers")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath().getList("description", String.class);

    // Assert
    assertThat(descriptions).containsExactly("Sooner", "Later");
  }

  private String createRecurringTransfer(final UUID userSid, final Account sourceAccount,
                                         final Account destinationAccount,
                                         final String description, final BigDecimal amount,
                                         final LocalDate startDate) {

    final CreateRecurringTransferRequestDto request = new CreateRecurringTransferRequestDto(
        sourceAccount.getSid(), destinationAccount.getSid(), description, amount, Frequency.MONTHLY, 15, null, true,
        startDate, null);

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
    return setupAccountWithInstitution(userSid, name);
  }

  private Account setupAccountWithInstitution(final UUID userSid, final String institution) {
    final Account account = Account.create(BigDecimal.ZERO, Currency.EUR, institution);
    account.addMembership(AccountMembership.create(userSid, MembershipRole.OWNER));
    return accountRepository.save(account);
  }

}
