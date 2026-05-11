package me.ferreira.graveto.moneytracker.transactions;

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
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.UpdateTransactionRequestDTO;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import me.ferreira.graveto.moneytracker.utils.TransactionTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.is;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UpdateTransactionIT extends MoneyTrackerBaseIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldUpdateTransaction() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final List<Category> categoryList = categoryRepository.findAllByUserSid(userSid);
        final Category initialCategory = categoryList.getFirst();
        final Category newCategory = categoryList.getLast();
        final BigDecimal initialBalance = BigDecimal.valueOf(100);
        final LocalDateTime newOccurredAt = LocalDateTime.now().minusDays(2);

        final Account account = AccountTestFactory.createAccountWithOwner(userSid, "Santander", initialBalance);
        accountRepository.save(account);

        final Transaction transaction = TransactionTestFactory.createTransaction(account, initialCategory, TransactionType.INCOME,
                BigDecimal.TEN, TransactionStatus.ACTIVE, LocalDate.now());
        transactionRepository.save(transaction);

        final UpdateTransactionRequestDTO requestDTO = new UpdateTransactionRequestDTO(
                TransactionType.EXPENSE,
                newCategory.getSid(),
                BigDecimal.valueOf(20),
                "New description",
                newOccurredAt
        );

        // Act
        given().
                header("Authorization", "Bearer " + userSid).
                pathParam("sid", transaction.getSid()).
                contentType(ContentType.JSON).
                body(requestDTO).
        when().
                patch("/transactions/{sid}").
        then().
                log().ifValidationFails().
                statusCode(200).
                body("sid", is(transaction.getSid().toString())).
                body("amount", is(20)).
                body("categoryName", is(newCategory.getDisplayName())).
                body("description", is("New description")).
                body("type", is(TransactionType.EXPENSE.name()));

        // Assert
        final Transaction updatedTransaction = transactionRepository.findBySid(transaction.getSid()).get();
        final Account updatedAccount = accountRepository.findBySid(account.getSid()).get();

        assertThat(updatedTransaction.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(updatedTransaction.getDescription()).isEqualTo("New description");
        assertThat(updatedTransaction.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(updatedTransaction.getOccurredAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(newOccurredAt.truncatedTo(ChronoUnit.MILLIS));
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(70));
    }

}
