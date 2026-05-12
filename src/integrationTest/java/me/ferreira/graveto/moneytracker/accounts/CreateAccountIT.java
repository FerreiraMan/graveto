package me.ferreira.graveto.moneytracker.accounts;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.accounts.web.dto.request.CreateAccountRequestDto;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CreateAccountIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private TransactionRepository transactionRepository;

  @Test
  void shouldCreateAccount() {
    // Arrange
    final BigDecimal expectedBalance = BigDecimal.TEN;
    final String expectedInstitution = "Santander";
    final CreateAccountRequestDto requestDto = new CreateAccountRequestDto(
        expectedBalance, Currency.EUR, expectedInstitution
    );
    final UUID userSid = UUID.randomUUID();

    // Act
    final String accountSid =
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + userSid)
            .body(requestDto)
            .when()
            .post("/accounts")
            .then()
            .statusCode(201)
            .body("status", equalTo(AccountStatus.ACTIVE.name()))
            .extract()
            .path("sid");

    // Assert
    final Optional<Account> accountOptional = accountRepository.findBySidWithMemberships(UUID.fromString(accountSid));
    assertThat(accountOptional).isPresent();

    final Account savedAccount = accountOptional.get();
    assertThat(savedAccount.getBalance()).isEqualByComparingTo(expectedBalance);
    assertThat(savedAccount.getInstitution()).isEqualTo(expectedInstitution);
    assertThat(savedAccount.getMemberships()).isNotNull();
    assertThat(savedAccount.getMemberships().getFirst().getUserSid()).isEqualTo(userSid);

    final Page<Transaction> transactionsPage = transactionRepository.findAllByAccountId(
        savedAccount.getId(), Pageable.ofSize(1)
    );
    assertThat(transactionsPage.getTotalElements()).isEqualTo(1);

    final Transaction openingTx = transactionsPage.getContent().get(0);
    assertThat(openingTx.getAmount()).isEqualByComparingTo(expectedBalance);
    assertThat(openingTx.getType()).isEqualTo(TransactionType.OPENING_BALANCE);
  }

}
