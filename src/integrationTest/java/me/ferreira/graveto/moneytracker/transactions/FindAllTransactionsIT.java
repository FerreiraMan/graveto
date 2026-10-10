package me.ferreira.graveto.moneytracker.transactions;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.categories.service.command.FindAllCategoriesCommand;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import me.ferreira.graveto.moneytracker.utils.CategoryTestFactory;
import me.ferreira.graveto.moneytracker.utils.TransactionTestFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql", "/moneytracker/sql/delete_find_all_transactions_categories.sql"},
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
public class FindAllTransactionsIT extends MoneyTrackerBaseIntegrationTest {

  private static final String CATEGORY_PREFIX = "FindAllTxIT-";

  private static final UUID ACCOUNT_OWNER = UUID.randomUUID();
  private static final UUID SECOND_ACCOUNT_OWNER = UUID.randomUUID();
  private static final Account ACCOUNT_1 =
      AccountTestFactory.createAccountWithOwner(ACCOUNT_OWNER, "BCP", BigDecimal.TEN);
  private static final Account ACCOUNT_2 =
      AccountTestFactory.createAccountWithOwner(SECOND_ACCOUNT_OWNER, "BPI", BigDecimal.ONE);

  private static final UUID TREE_OWNER = UUID.randomUUID();
  private static final UUID TREE_CONTRIBUTOR = UUID.randomUUID();
  private static final Account TREE_ACCOUNT =
      AccountTestFactory.createAccountWithOwner(TREE_OWNER, "Tree bank", BigDecimal.TEN);

  static {
    TREE_ACCOUNT.addMembership(AccountMembership.create(TREE_CONTRIBUTOR, MembershipRole.CONTRIBUTOR));
  }

  @Autowired
  private TransactionRepository transactionRepository;
  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private CategoryRepository categoryRepository;

  private Category firstCategory;
  private Category secondCategory;
  private List<Transaction> allTransactions;
  private Transaction guaranteedMatch;
  private Transaction guaranteedDeletedMatch;

  // root -> child -> grandChild, plus an unrelated sibling root, all scoped to TREE_ACCOUNT
  private Category root;
  private Category child;
  private Category grandChild;
  private Category sibling;
  // system category (no account) with one child owned by TREE_ACCOUNT and one owned by ACCOUNT_2
  private Category systemParent;
  private Category treeAccountChildOfSystemParent;
  private Category otherAccountChildOfSystemParent;
  private Transaction rootTx;
  private Transaction childTx;
  private Transaction grandChildTx;
  private Transaction siblingTx;

  @BeforeAll
  void setupData() {
    accountRepository.saveAll(List.of(ACCOUNT_1, ACCOUNT_2, TREE_ACCOUNT));

    final List<Category> categoryList =
        categoryRepository.findAll(new FindAllCategoriesCommand(null, null, null, null, null));
    // Filtering by a category also returns its descendants, so the two noise categories must be root categories to
    // guarantee that neither one is a descendant of the other.
    firstCategory =
        categoryList.stream().filter(c -> !c.isInternal() && c.getParent() == null).findAny().orElseThrow();
    secondCategory = categoryList.stream()
        .filter(c -> !c.isInternal() && c.getParent() == null && !c.getSid().equals(firstCategory.getSid()))
        .findFirst()
        .orElseThrow();

    guaranteedMatch = TransactionTestFactory.createTransaction(
        ACCOUNT_1,
        firstCategory,
        TransactionType.EXPENSE,
        new BigDecimal("999.99"),
        TransactionStatus.ACTIVE,
        LocalDate.now().minusDays(5)
    );

    guaranteedDeletedMatch = TransactionTestFactory.createTransaction(
        ACCOUNT_1,
        firstCategory,
        TransactionType.EXPENSE,
        new BigDecimal("111.11"),
        TransactionStatus.DELETED,
        LocalDate.now().minusDays(2)
    );

    final List<Account> accounts = List.of(ACCOUNT_1, ACCOUNT_2);
    final List<Category> categories = List.of(firstCategory, secondCategory);
    final TransactionType[] types = TransactionType.values();
    final TransactionStatus[] statuses = TransactionStatus.values();
    final ThreadLocalRandom random = ThreadLocalRandom.current();

    final List<Transaction> noise = IntStream.range(0, 50)
        .mapToObj(i -> TransactionTestFactory.createTransaction(
            accounts.get(random.nextInt(accounts.size())),
            categories.get(random.nextInt(categories.size())),
            types[random.nextInt(types.length)],
            BigDecimal.valueOf(random.nextDouble(1.0, 500.0)).setScale(2, RoundingMode.HALF_UP),
            statuses[random.nextInt(statuses.length)],
            LocalDate.now().minusDays(random.nextInt(0, 31))
        ))
        .collect(Collectors.toList());

    noise.add(guaranteedMatch);
    noise.add(guaranteedDeletedMatch);
    allTransactions = transactionRepository.saveAll(noise);

    setupCategoryTree();
  }

  private void setupCategoryTree() {
    root = newCategory("root", TREE_ACCOUNT.getSid(), null);
    child = newCategory("child", TREE_ACCOUNT.getSid(), root);
    grandChild = newCategory("grandChild", TREE_ACCOUNT.getSid(), child);
    sibling = newCategory("sibling", TREE_ACCOUNT.getSid(), null);

    systemParent = newCategory("systemParent", null, null);
    treeAccountChildOfSystemParent = newCategory("treeAccountChild", TREE_ACCOUNT.getSid(), systemParent);
    otherAccountChildOfSystemParent = newCategory("otherAccountChild", ACCOUNT_2.getSid(), systemParent);

    rootTx = newTransaction(TREE_ACCOUNT, root, "1.01");
    childTx = newTransaction(TREE_ACCOUNT, child, "2.02");
    grandChildTx = newTransaction(TREE_ACCOUNT, grandChild, "3.03");
    siblingTx = newTransaction(TREE_ACCOUNT, sibling, "4.04");
    newTransaction(TREE_ACCOUNT, systemParent, "5.05");
    newTransaction(TREE_ACCOUNT, treeAccountChildOfSystemParent, "6.06");
    // belongs to another account, must never show up in TREE_ACCOUNT's results
    newTransaction(ACCOUNT_2, otherAccountChildOfSystemParent, "7.07");
  }

  private Category newCategory(final String name, final UUID accountSid, final Category parent) {
    return categoryRepository.save(
        CategoryTestFactory.createCategory(CATEGORY_PREFIX + name, accountSid, parent, false));
  }

  private Transaction newTransaction(final Account account, final Category category, final String amount) {
    return transactionRepository.save(TransactionTestFactory.createTransaction(
        account, category, TransactionType.EXPENSE, new BigDecimal(amount), TransactionStatus.ACTIVE,
        LocalDate.now().minusDays(1)));
  }

  private List<String> fetchSids(final UUID user, final UUID accountSid, final UUID categorySid) {
    return given()
        .header("Authorization", "Bearer " + user)
        .queryParam("accountSid", accountSid)
        .queryParam("categorySid", categorySid)
        .queryParam("size", 100)
        .when()
        .get("/transactions")
        .then()
        .log().ifValidationFails()
        .statusCode(200)
        .extract()
        .path("content.sid");
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
        .filter(t -> t.getStatus() == TransactionStatus.ACTIVE)
        .filter(t -> !t.getOccurredAt().toLocalDate().isBefore(startDate))
        .filter(t -> !t.getOccurredAt().toLocalDate().isAfter(endDate))
        .count();

    // Act & Assert
    given()
        .header("Authorization", "Bearer " + ACCOUNT_OWNER)
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
        .body("content.every { it.category.name == '" + firstCategory.getDisplayName() + "' }", is(true))
        .body("content.every { it.status == 'ACTIVE' }", is(true));
  }

  @Test
  void shouldReturnOnlyDeletedTransactionsWhenRequested() {
    // Arrange
    final UUID targetAccountSid = ACCOUNT_1.getSid();
    final TransactionStatus targetStatus = TransactionStatus.DELETED;

    long expectedCount = allTransactions.stream()
        .filter(t -> t.getAccount().getSid().equals(targetAccountSid))
        .filter(t -> t.getStatus() == targetStatus)
        .count();

    // Act & Assert
    given()
        .header("Authorization", "Bearer " + ACCOUNT_OWNER)
        .queryParam("accountSid", targetAccountSid)
        .queryParam("status", targetStatus.name())
        .queryParam("size", 100)
        .when()
        .get("/transactions")
        .then()
        .log().ifValidationFails()
        .statusCode(200)
        .body("totalElements", is((int) expectedCount))
        .body("content.sid", hasItem(guaranteedDeletedMatch.getSid().toString()))
        .body("content.sid", not(hasItem(guaranteedMatch.getSid().toString())))
        .body("content.every { it.status == '" + targetStatus.name() + "' }", is(true));
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
            .header("Authorization", "Bearer " + ACCOUNT_OWNER)
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

  @Test
  void shouldReturnTransactionsOfTheCategoryAndAllOfItsDescendants() {
    // Act
    final List<String> sids = fetchSids(TREE_OWNER, TREE_ACCOUNT.getSid(), root.getSid());

    // Assert
    assertThat(sids).containsExactlyInAnyOrder(
        rootTx.getSid().toString(), childTx.getSid().toString(), grandChildTx.getSid().toString());
    assertThat(sids).doesNotContain(siblingTx.getSid().toString());
  }

  @Test
  void shouldReturnOnlyTheSubtreeWhenFilteringByAnIntermediateCategory() {
    // Act
    final List<String> sids = fetchSids(TREE_OWNER, TREE_ACCOUNT.getSid(), child.getSid());

    // Assert
    assertThat(sids).containsExactlyInAnyOrder(childTx.getSid().toString(), grandChildTx.getSid().toString());
  }

  @Test
  void shouldReturnOnlyTheOwnTransactionsWhenFilteringByLeafCategory() {
    // Act
    final List<String> sids = fetchSids(TREE_OWNER, TREE_ACCOUNT.getSid(), grandChild.getSid());

    // Assert
    assertThat(sids).containsExactly(grandChildTx.getSid().toString());
  }

  @Test
  void shouldNotIncludeDescendantsOwnedByAnotherAccountWhenFilteringBySystemCategory() {
    // Act
    final List<String> categoryNames =
        given()
            .header("Authorization", "Bearer " + TREE_OWNER)
            .queryParam("accountSid", TREE_ACCOUNT.getSid())
            .queryParam("categorySid", systemParent.getSid())
            .queryParam("size", 100)
            .when()
            .get("/transactions")
            .then()
            .statusCode(200)
            .extract()
            .path("content.category.name");

    // Assert
    assertThat(categoryNames).containsExactlyInAnyOrder(
        CATEGORY_PREFIX + "systemParent", CATEGORY_PREFIX + "treeAccountChild");
  }

  @Test
  void shouldResolveOnlyTheDescendantsVisibleToTheAccount() {
    // Act & Assert
    final List<String> names = categoryRepository
        .findCategoryAndAllDescendants(systemParent.getSid(), TREE_ACCOUNT.getSid()).stream()
        .map(Category::getName)
        .toList();

    assertThat(names).containsExactlyInAnyOrder(
        CATEGORY_PREFIX + "systemParent", CATEGORY_PREFIX + "treeAccountChild");
  }

  @Test
  void shouldReturnNoTransactionsWhenTheCategoryDoesNotExist() {
    // Act & Assert
    given()
        .header("Authorization", "Bearer " + TREE_OWNER)
        .queryParam("accountSid", TREE_ACCOUNT.getSid())
        .queryParam("categorySid", UUID.randomUUID())
        .when()
        .get("/transactions")
        .then()
        .statusCode(200)
        .body("totalElements", is(0))
        .body("content", empty());
  }

  @Test
  void shouldReturnNoTransactionsWhenTheCategoryBelongsToAnotherAccount() {
    // Arrange
    final Category foreignRoot = otherAccountChildOfSystemParent;

    // Act & Assert
    given()
        .header("Authorization", "Bearer " + TREE_OWNER)
        .queryParam("accountSid", TREE_ACCOUNT.getSid())
        .queryParam("categorySid", foreignRoot.getSid())
        .when()
        .get("/transactions")
        .then()
        .statusCode(200)
        .body("totalElements", is(0))
        .body("content", empty());
  }

  @Test
  void shouldReturnEveryTransactionOfTheAccountWhenNoCategoryIsRequested() {
    // Act & Assert
    given()
        .header("Authorization", "Bearer " + TREE_OWNER)
        .queryParam("accountSid", TREE_ACCOUNT.getSid())
        .queryParam("size", 100)
        .when()
        .get("/transactions")
        .then()
        .statusCode(200)
        .body("totalElements", is(6))
        .body("content.sid", hasItem(siblingTx.getSid().toString()));
  }

  @Test
  void shouldForbidUserWhoIsNotMemberOfTheAccount() {
    // Act & Assert
    given()
        .header("Authorization", "Bearer " + SECOND_ACCOUNT_OWNER)
        .queryParam("accountSid", TREE_ACCOUNT.getSid())
        .when()
        .get("/transactions")
        .then()
        .statusCode(403)
        .body("detail", is("You do not have the required role to perform this action."))
        .body("content", nullValue());
  }

  @Test
  void shouldForbidNonMemberEvenWhenAskingForCategoryTree() {
    // Act & Assert
    given()
        .header("Authorization", "Bearer " + SECOND_ACCOUNT_OWNER)
        .queryParam("accountSid", TREE_ACCOUNT.getSid())
        .queryParam("categorySid", root.getSid())
        .when()
        .get("/transactions")
        .then()
        .statusCode(403)
        .body("detail", is("You do not have the required role to perform this action."));
  }

  @Test
  void shouldReturnNotFoundWhenTheAccountDoesNotExist() {
    // Act & Assert
    given()
        .header("Authorization", "Bearer " + TREE_OWNER)
        .queryParam("accountSid", UUID.randomUUID())
        .when()
        .get("/transactions")
        .then()
        .statusCode(404)
        .body("detail", is(
            "The specified account is no longer available. " +
                "It may have been removed, or you may not have access to it."));
  }

}
