package me.ferreira.graveto.moneytracker.transactions.transfer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transfer.UpdateTransferRequestDto;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import me.ferreira.graveto.moneytracker.utils.TransactionTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UpdateTransferIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private TransactionRepository transactionRepository;
  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private CategoryRepository categoryRepository;

  @Test
  void shouldUpdateTransferTransactionsAndAccountBalances() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Category category = categoryRepository.findByAccountSidIsNull().getFirst();

    final BigDecimal persistedTransferAmount = BigDecimal.valueOf(50);
    final BigDecimal currentSourceBalance = BigDecimal.valueOf(150);
    final BigDecimal currentDestBalance = BigDecimal.valueOf(150);

    final Account outAccount = AccountTestFactory.createAccountWithOwner(userSid, "Checking", currentSourceBalance);
    final Account inAccount = AccountTestFactory.createAccountWithOwner(userSid, "Savings", currentDestBalance);
    accountRepository.saveAll(List.of(outAccount, inAccount));

    final UUID correlationId = UUID.randomUUID();
    final Transaction outTx =
        TransactionTestFactory.createTransaction(outAccount, category, TransactionType.TRANSFER_OUT,
            persistedTransferAmount, TransactionStatus.ACTIVE, LocalDate.now());
    outTx.setCorrelationId(correlationId);

    final Transaction inTx = TransactionTestFactory.createTransaction(inAccount, category, TransactionType.TRANSFER_IN,
        persistedTransferAmount, TransactionStatus.ACTIVE, LocalDate.now());
    inTx.setCorrelationId(correlationId);

    transactionRepository.saveAll(List.of(outTx, inTx));

    final BigDecimal newTransferAmount = BigDecimal.valueOf(80);
    final String newDescription = "Corrected Transfer Amount";
    final LocalDateTime newOccurredAt = LocalDateTime.now().minusDays(2);

    final UpdateTransferRequestDto requestDto = new UpdateTransferRequestDto(
        newTransferAmount,
        newDescription,
        newOccurredAt
    );

    // Act
    given()
        .header("Authorization", "Bearer " + userSid)
        .pathParam("correlationId", correlationId)
        .contentType(ContentType.JSON)
        .body(requestDto)
        .when()
        .patch("/transfers/{correlationId}")
        .then()
        .log().ifValidationFails()
        .statusCode(200)
        .body("correlationId", is(correlationId.toString()));

    // Assert
    final List<Transaction> updatedTransactions = transactionRepository.findAllByCorrelationId(correlationId);
    assertThat(updatedTransactions.size()).isEqualTo(2);

    final Transaction updatedOutTx =
        updatedTransactions.stream().filter(t -> t.getType() == TransactionType.TRANSFER_OUT).findFirst().get();
    final Transaction updatedInTx =
        updatedTransactions.stream().filter(t -> t.getType() == TransactionType.TRANSFER_IN).findFirst().get();

    assertThat(updatedOutTx.getAmount()).isEqualByComparingTo(newTransferAmount);
    assertThat(updatedOutTx.getDescription()).isEqualTo(newDescription);
    assertThat(updatedOutTx.getOccurredAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(
        newOccurredAt.truncatedTo(ChronoUnit.MILLIS));

    assertThat(updatedInTx.getAmount()).isEqualByComparingTo(newTransferAmount);
    assertThat(updatedInTx.getDescription()).isEqualTo(newDescription);
    assertThat(updatedInTx.getOccurredAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(
        newOccurredAt.truncatedTo(ChronoUnit.MILLIS));

    final Account updatedOutAccount = accountRepository.findBySid(outAccount.getSid()).get();
    final Account updatedInAccount = accountRepository.findBySid(inAccount.getSid()).get();

    assertThat(updatedOutAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(120));
    assertThat(updatedInAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(180));
  }

}
