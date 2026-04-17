package me.ferreira.graveto.moneytracker.transactions.transfer;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.config.BaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transfer.CreateTransferRequestDTO;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.is;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CreateTransferIT extends BaseIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldCreateTransfer() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final BigDecimal sourceAccountInitialBalance = BigDecimal.valueOf(200);
        final BigDecimal destinationAccountInitialBalance = BigDecimal.valueOf(100);

        final BigDecimal transferAmount = BigDecimal.valueOf(45);
        final String description = "Money to invest";

        final Account sourceAccount = AccountTestFactory.createAccountWithOwner(userSid, "Santander", sourceAccountInitialBalance);
        final Account destinationAccount = AccountTestFactory.createAccountWithOwner(userSid, "BCP", destinationAccountInitialBalance);
        accountRepository.saveAll(List.of(sourceAccount, destinationAccount));

        final CreateTransferRequestDTO requestDTO = new CreateTransferRequestDTO(
                sourceAccount.getSid(),
                destinationAccount.getSid(),
                transferAmount,
                description,
                LocalDateTime.now()
        );

        // Act
        final Response response = given().
                header("X-User-Sid", userSid).
                contentType(ContentType.JSON).
                body(requestDTO).
                when().
                post("/transfers");
        response.then().
                log().ifValidationFails().
                statusCode(201).
                body("sourceAccountSid", is(sourceAccount.getSid().toString())).
                body("destinationAccountSid", is(destinationAccount.getSid().toString())).
                body("amount", is(transferAmount.intValue()));

        // Assert
        final String correlationId = response.path("correlationId");
        assertThat(correlationId).isNotNull();

        assertThat(response.header("Location")).endsWith("/transfers/" + correlationId);

        final List<Transaction> transactions = transactionRepository.findAllByCorrelationId(UUID.fromString(correlationId));
        assertThat(transactions.size()).isEqualTo(2);

        final Transaction outTx = transactions.stream().filter(t -> t.getType() == TransactionType.TRANSFER_OUT).findFirst().orElseThrow();
        final Transaction inTx = transactions.stream().filter(t -> t.getType() == TransactionType.TRANSFER_IN).findFirst().orElseThrow();
        assertThat(outTx.getAmount()).isEqualByComparingTo(transferAmount);
        assertThat(outTx.getAccount().getSid()).isEqualTo(sourceAccount.getSid());
        assertThat(inTx.getAmount()).isEqualByComparingTo(transferAmount);
        assertThat(inTx.getAccount().getSid()).isEqualTo(destinationAccount.getSid());

        final Account updatedSourceAccount = accountRepository.findBySid(sourceAccount.getSid()).get();
        final Account updatedDestinationAccount = accountRepository.findBySid(destinationAccount.getSid()).get();
        assertThat(updatedSourceAccount.getBalance()).isEqualByComparingTo(sourceAccountInitialBalance.subtract(transferAmount));
        assertThat(updatedDestinationAccount.getBalance()).isEqualByComparingTo(destinationAccountInitialBalance.add(transferAmount));
    }

}
