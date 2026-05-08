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
import static org.hamcrest.Matchers.is;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FetchTransferIT extends MoneyTrackerBaseIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldFetchTransferAndMapToResponseDTO() {
        // Arrange
        final UUID userSid = UUID.randomUUID();

        final Category category = categoryRepository.findByUserSidIsNull().getFirst();

        final Account outAccount = AccountTestFactory.createAccountWithOwner(userSid, "Checking", BigDecimal.valueOf(500));
        final Account inAccount = AccountTestFactory.createAccountWithOwner(userSid, "Savings", BigDecimal.valueOf(100));
        accountRepository.saveAll(List.of(outAccount, inAccount));

        final BigDecimal transferAmount = BigDecimal.valueOf(150);
        final UUID correlationId = UUID.randomUUID();

        final Transaction outTx = TransactionTestFactory.createTransaction(outAccount, category, TransactionType.TRANSFER_OUT,
                transferAmount, TransactionStatus.ACTIVE, LocalDate.now());
        outTx.setCorrelationId(correlationId);

        final Transaction inTx = TransactionTestFactory.createTransaction(inAccount, category, TransactionType.TRANSFER_IN,
                transferAmount, TransactionStatus.ACTIVE, LocalDate.now());
        inTx.setCorrelationId(correlationId);

        transactionRepository.saveAll(List.of(outTx, inTx));

        // Act & Assert
        given().
                header("X-User-Sid", userSid).
                pathParam("correlationId", correlationId).
                contentType(ContentType.JSON).
        when().
                get("/transfers/{correlationId}").
        then().
                log().ifValidationFails().
                statusCode(200).
                body("sourceAccountSid", is(outAccount.getSid().toString())).
                body("destinationAccountSid", is(inAccount.getSid().toString())).
                body("amount", is(transferAmount.floatValue())).
                body("correlationId", is(correlationId.toString())).
                body("transferStatus", is(TransactionStatus.ACTIVE.name()));
    }

}
