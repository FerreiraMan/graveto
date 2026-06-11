package me.ferreira.graveto.moneytracker.transactions;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.CreateTransactionRequestDto;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CreateTransactionIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private TransactionRepository transactionRepository;
  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private CategoryRepository categoryRepository;

  @Test
  void shouldCreateTransaction() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Category category = categoryRepository.findAllByAccountSid(userSid).getFirst();
    final BigDecimal initialBalance = BigDecimal.TEN;

    final Account account = AccountTestFactory.createAccountWithOwner(userSid, "Santander", initialBalance);
    accountRepository.save(account);

    final BigDecimal transactionAmount = BigDecimal.TEN;
    final String description = "Lunch with friends";

    final CreateTransactionRequestDto requestDto = new CreateTransactionRequestDto(
        account.getSid(),
        category.getSid(),
        transactionAmount,
        description,
        TransactionType.EXPENSE,
        LocalDateTime.now()
    );

    // Act
    final String transactionSid =
        given()
            .header("Authorization", "Bearer " + userSid)
            .contentType(ContentType.JSON)
            .body(requestDto)
            .when()
            .post("/transactions")
            .then()
            .statusCode(201)
            .extract()
            .path("sid");

    // Assert
    assertThat(transactionSid).isNotNull();

    final PageRequest pageRequest = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "occurredAt"));
    final Page<Transaction> transactionPage = transactionRepository.findAllByAccountId(account.getId(), pageRequest);
    final Transaction transaction = transactionPage.getContent().get(0);

    assertThat(transaction.getAmount()).isEqualByComparingTo(transactionAmount);
    assertThat(transaction.getType()).isEqualTo(TransactionType.EXPENSE);

    final Account updatedAccount = accountRepository.findBySid(account.getSid()).get();
    assertThat(updatedAccount.getBalance()).isEqualByComparingTo(initialBalance.subtract(transactionAmount));
  }

}
