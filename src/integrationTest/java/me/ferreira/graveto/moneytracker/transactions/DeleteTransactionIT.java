package me.ferreira.graveto.moneytracker.transactions;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import me.ferreira.graveto.moneytracker.utils.TransactionTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class DeleteTransactionIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private TransactionRepository transactionRepository;
  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private CategoryRepository categoryRepository;

  @Test
  void shouldDeleteTransaction() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Category category = categoryRepository.findAllByAccountSid(userSid).getFirst();
    final BigDecimal initialBalance = BigDecimal.TEN;

    final Account account = AccountTestFactory.createAccountWithOwner(userSid, "Santander", initialBalance);
    accountRepository.save(account);

    final Transaction transaction = TransactionTestFactory.createTransaction(account, category, TransactionType.EXPENSE,
        BigDecimal.TEN, TransactionStatus.ACTIVE, LocalDate.now());
    transactionRepository.save(transaction);

    // Act
    given()
        .header("Authorization", "Bearer " + userSid)
        .pathParam("sid", transaction.getSid())
        .contentType(ContentType.JSON)
        .when()
        .delete("/transactions/{sid}")
        .then()
        .log().ifValidationFails()
        .statusCode(200)
        .body("sid", is(transaction.getSid().toString()))
        .body("status", is(TransactionStatus.DELETED.name()));

    // Assert
    final Transaction deletedTransaction = transactionRepository.findBySid(transaction.getSid()).get();
    final Account updatedAccount = accountRepository.findBySid(account.getSid()).get();

    assertThat(deletedTransaction.getStatus()).isEqualTo(TransactionStatus.DELETED);
    assertThat(deletedTransaction.getDeletedAt()).isNotNull();
    assertThat(updatedAccount.getBalance()).isEqualByComparingTo(initialBalance.add(transaction.getAmount()));
  }

}
