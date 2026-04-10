package me.ferreira.graveto.moneytracker.transacations;

import io.restassured.http.ContentType;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.config.BaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import me.ferreira.graveto.moneytracker.utils.TransactionTestFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.*;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
public class FindAllTransactionsIT extends BaseIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    private static final UUID ACCOUNT_OWNER = UUID.randomUUID();
    private static final UUID SECOND_ACCOUNT_OWNER = UUID.randomUUID();
    private static final Account ACCOUNT_1 = AccountTestFactory.createAccountWithOwner(ACCOUNT_OWNER, "BCP", BigDecimal.TEN);
    private static final Account ACCOUNT_2 = AccountTestFactory.createAccountWithOwner(SECOND_ACCOUNT_OWNER, "BPI", BigDecimal.ONE);

    private Category firstCategory;
    private Category secondCategory;

    private List<Transaction> allTransactions;
    private Transaction guaranteedMatch;

    @BeforeAll
    void setupData() {
        accountRepository.saveAll(List.of(ACCOUNT_1, ACCOUNT_2));

        final List<Category> categoryList = categoryRepository.findByUserSidIsNull();
        firstCategory = categoryList.stream().filter(c -> !c.isInternal()).findAny().orElseThrow();
        secondCategory = categoryList.stream().filter(c -> !c.isInternal() && !c.getSid().equals(firstCategory.getSid())).findFirst().orElseThrow();

        guaranteedMatch = TransactionTestFactory.createTransaction(
                ACCOUNT_1,
                firstCategory,
                TransactionType.EXPENSE,
                new BigDecimal("999.99"),
                TransactionStatus.ACTIVE,
                LocalDate.now().minusDays(5)
        );

        final List<Account> accounts = List.of(ACCOUNT_1, ACCOUNT_2);
        final List<Category> categories = List.of(firstCategory, secondCategory);
        final TransactionType[] types = TransactionType.values();
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        final List<Transaction> noise = IntStream.range(0, 50)
            .mapToObj(i -> TransactionTestFactory.createTransaction(
                    accounts.get(random.nextInt(accounts.size())),
                    categories.get(random.nextInt(categories.size())),
                    types[random.nextInt(types.length)],
                    BigDecimal.valueOf(random.nextDouble(1.0, 500.0)).setScale(2, RoundingMode.HALF_UP),
                    TransactionStatus.ACTIVE,
                    LocalDate.now().minusDays(random.nextInt(0, 31))
            ))
            .collect(Collectors.toList());

        noise.add(guaranteedMatch);
        allTransactions = transactionRepository.saveAll(noise);
    }

    @Test
    void shouldReturnTransactionsAccordingToFilter() {
        // Arrange
        final UUID targetAccountSid = ACCOUNT_1.getSid();
        final UUID targetCategorySid = firstCategory.getSid();
        final TransactionType targetType = TransactionType.EXPENSE;
        final LocalDate startDate = LocalDate.now().minusDays(31);
        final LocalDate endDate = LocalDate.now();

        long expectedCount = allTransactions.stream()
                .filter(t -> t.getAccount().getSid().equals(targetAccountSid))
                .filter(t -> t.getCategory().getSid().equals(targetCategorySid))
                .filter(t -> t.getType() == targetType)
                .filter(t -> !t.getOccurredAt().toLocalDate().isBefore(startDate))
                .filter(t -> !t.getOccurredAt().toLocalDate().isAfter(endDate))
                .count();

        // Act & Assert
        given()
                .header("X-User-Sid", ACCOUNT_OWNER)
                .queryParam("accountSid", targetAccountSid)
                .queryParam("categorySid", targetCategorySid)
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .queryParam("type", targetType.name())
                .queryParam("size", 100)
        .when()
                .get("/transactions")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)

                .body("totalElements", is((int) expectedCount))
                .body("content.sid", hasItem(guaranteedMatch.getSid().toString()))
                .body("content.every { it.type == '" + targetType.name() + "' }", is(true))
                .body("content.every { it.categoryName == '" + firstCategory.getDisplayName() + "' }", is(true));
    }

    @Test
    void shouldReturnTransactionsSortedByOccurredAtDescendingByDefault() {
        // Arrange
        final Transaction olderTx = TransactionTestFactory.createTransaction(
                ACCOUNT_1,
                firstCategory,
                TransactionType.EXPENSE,
                BigDecimal.TEN,
                TransactionStatus.ACTIVE,
                LocalDate.now().minusDays(20)
        );

        transactionRepository.save(olderTx);

        // Act
        final List<String> sids =
                given()
                    .header("X-User-Sid", ACCOUNT_OWNER)
                    .queryParam("accountSid", ACCOUNT_1.getSid())
                    .queryParam("categorySid", firstCategory.getSid())
                    .queryParam("type", TransactionType.EXPENSE.name())
                .when()
                    .get("/transactions")
                .then()
                    .statusCode(200)
                    .extract()
                    .path("content.sid");

        // Assert
        int indexOfNewer = sids.indexOf(guaranteedMatch.getSid().toString());
        int indexOfOlder = sids.indexOf(olderTx.getSid().toString());

        assertThat(indexOfNewer).isLessThan(indexOfOlder);
    }

}
