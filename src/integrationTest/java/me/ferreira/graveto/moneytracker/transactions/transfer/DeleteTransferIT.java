package me.ferreira.graveto.moneytracker.transactions.transfer;

import io.restassured.http.ContentType;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.is;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class DeleteTransferIT extends MoneyTrackerBaseIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldDeleteTransferTransactions() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final Category category = categoryRepository.findAllByUserSid(userSid).getFirst();
        final BigDecimal firstAccountInitialBalance = BigDecimal.valueOf(100);
        final BigDecimal secondAccountInitialBalance = BigDecimal.valueOf(200);

        final Account outAccount = AccountTestFactory.createAccountWithOwner(userSid, "Santander", firstAccountInitialBalance);
        final Account inAccount = AccountTestFactory.createAccountWithOwner(userSid, "Santander", secondAccountInitialBalance);
        accountRepository.saveAll(List.of(outAccount, inAccount));

        final Transaction outTx = TransactionTestFactory.createTransaction(outAccount, category, TransactionType.TRANSFER_OUT,
                BigDecimal.TEN, TransactionStatus.ACTIVE, LocalDate.now());
        final Transaction inTx = TransactionTestFactory.createTransaction(inAccount, category, TransactionType.TRANSFER_IN,
                BigDecimal.TEN, TransactionStatus.ACTIVE, LocalDate.now());

        final UUID correlationId = UUID.randomUUID();
        outTx.setCorrelationId(correlationId);
        inTx.setCorrelationId(correlationId);

        transactionRepository.saveAll(List.of(outTx, inTx));

        // Act
        given().
                header("Authorization", "Bearer " + userSid).
                pathParam("correlationId", correlationId).
                contentType(ContentType.JSON).
        when().
                delete("/transfers/{correlationId}").
        then().
                log().ifValidationFails().
                statusCode(200).
                body("sourceAccountSid", is(outAccount.getSid().toString())).
                body("destinationAccountSid", is(inAccount.getSid().toString())).
                body("amount", is(outTx.getAmount().floatValue())).
                body("correlationId", is(correlationId.toString())).
                body("transferStatus", is(TransactionStatus.DELETED.name()));

        // Assert
        final List<Transaction> deletedTransferTransactions = transactionRepository.findAllByCorrelationId(correlationId);
        final Account updatedOutAccount = accountRepository.findBySid(outAccount.getSid()).get();
        final Account updatedInAccount = accountRepository.findBySid(inAccount.getSid()).get();

        assertThat(deletedTransferTransactions.size()).isEqualTo(2);

        assertThat(deletedTransferTransactions.get(0).getStatus()).isEqualTo(TransactionStatus.DELETED);
        assertThat(deletedTransferTransactions.get(0).getDeletedAt()).isNotNull();
        assertThat(deletedTransferTransactions.get(1).getStatus()).isEqualTo(TransactionStatus.DELETED);
        assertThat(deletedTransferTransactions.get(1).getDeletedAt()).isNotNull();

        assertThat(updatedOutAccount.getBalance()).isEqualByComparingTo(firstAccountInitialBalance.add(outTx.getAmount()));
        assertThat(updatedInAccount.getBalance()).isEqualByComparingTo(secondAccountInitialBalance.subtract(inTx.getAmount()));
    }

}
