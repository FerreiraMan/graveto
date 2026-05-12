package me.ferreira.graveto.moneytracker.accounts;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FetchAllAccountsIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private AccountRepository accountRepository;

  @Test
  void shouldReturnAllAccountsOfGivenUser() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final String institution = "BCP";
    final String institution2 = "Santander";
    final BigDecimal balance = BigDecimal.TEN;
    final BigDecimal balance2 = BigDecimal.TWO;
    final Account savedAccount =
        accountRepository.save(AccountTestFactory.createAccountWithOwner(userSid, institution, balance));
    final Account savedAccount2 =
        accountRepository.save(AccountTestFactory.createAccountWithOwner(userSid, institution2, balance2));

    // Act
    final List<String> extractedSids =
        given()
            .header("Authorization", "Bearer " + userSid)
            .when()
            .get("/accounts")
            .then()
            .statusCode(200)
            .extract()
            .path("sid");

    assertThat(extractedSids.size()).isEqualTo(2);
    assertThat(extractedSids).containsExactlyInAnyOrder(
        savedAccount.getSid().toString(),
        savedAccount2.getSid().toString()
    );
  }

  @Test
  void shouldReturnEmptyListIfUserHasNoAccounts() {
    // Arrange
    final UUID userSidWithAccount = UUID.randomUUID();
    final String institution = "BCP";
    final BigDecimal balance = BigDecimal.TEN;
    accountRepository.save(AccountTestFactory.createAccountWithOwner(userSidWithAccount, institution, balance));

    final UUID userSidWithoutAccount = UUID.randomUUID();

    // Act & Assert
    given()
        .header("Authorization", "Bearer " + userSidWithoutAccount)
        .when()
        .get("/accounts")
        .then()
        .statusCode(200)
        .body("size()", equalTo(0));
  }

}
