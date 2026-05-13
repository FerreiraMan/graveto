package me.ferreira.graveto.moneytracker.accounts;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CloseAccountIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private TransactionRepository transactionRepository;

  @Test
  void shouldCloseAccount() {
    // Arrange
    final BigDecimal balance = BigDecimal.ZERO;
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountTestFactory.createAccountWithOwner(userSid, "Santander", balance);
    assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    accountRepository.save(account);

    // Act
    final Response response = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .pathParam("sid", account.getSid())
        .when()
        .patch("/accounts/{sid}/close");

    // Assert
    response.then()
        .log().ifValidationFails()
        .statusCode(200)
        .body("sid", is(account.getSid().toString()))
        .body("status", is(AccountStatus.CLOSED.name()));

    final Account closedAccount = accountRepository.findBySid(account.getSid()).get();
    assertThat(closedAccount.getStatus()).isEqualTo(AccountStatus.CLOSED);
  }

}
