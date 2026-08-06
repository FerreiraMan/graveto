package me.ferreira.graveto.moneytracker.categories;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import me.ferreira.graveto.moneytracker.utils.CategoryTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {
    "/moneytracker/sql/delete_specific_categories.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FindAllCategoriesIT extends MoneyTrackerBaseIntegrationTest {

  final UUID ownerSid = UUID.randomUUID();
  private Account account;
  private Category parentCategory;
  private Category accountCategory;
  private Category incomeCategory;

  @Autowired
  private CategoryRepository categoryRepository;
  @Autowired
  private AccountRepository accountRepository;

  @BeforeEach
  void setupTestBaseline() {

    this.account = AccountTestFactory.createAccountWithOwner(ownerSid, "", BigDecimal.ONE);
    this.parentCategory =
        CategoryTestFactory.createCategory("Parent", null, null, false, TransactionType.EXPENSE);
    this.accountCategory =
        CategoryTestFactory.createCategory("Child", account.getSid(), parentCategory, false, TransactionType.EXPENSE);
    this.incomeCategory =
        CategoryTestFactory.createCategory("Salary", account.getSid(), null, false, TransactionType.INCOME);

    accountRepository.save(account);
    categoryRepository.saveAll(List.of(parentCategory, accountCategory, incomeCategory));
  }

  @Test
  void shouldReturnAllCategoriesFromSystemAndAccount() {
    // Act & Assert
    final List<String> extractedSids =
        given()
            .header("Authorization", "Bearer " + ownerSid)
            .queryParam("accountSid", account.getSid().toString())
            .when()
            .get("/categories")
            .then()
            .statusCode(200)
            .extract()
            .path("sid");

    assertThat(extractedSids).contains(
        parentCategory.getSid().toString(),
        accountCategory.getSid().toString(),
        incomeCategory.getSid().toString()
    );
  }

  @Test
  void shouldReturnAllCategoriesFromSystemAndNotAccountSpecific() {
    // Act & Assert
    final List<String> extractedSids =
        given()
            .header("Authorization", "Bearer " + ownerSid)
            .when()
            .get("/categories")
            .then()
            .statusCode(200)
            .extract()
            .path("sid");

    assertThat(extractedSids).contains(parentCategory.getSid().toString());
    assertThat(extractedSids).doesNotContain(accountCategory.getSid().toString(), incomeCategory.getSid().toString());
  }

  @Test
  void shouldFilterOutInternalCategoriesWhenAccountIsSpecified() {
    // Arrange
    final List<String> internalCategorySids = SystemCategory.allSids().stream()
        .map(UUID::toString)
        .toList();

    // Act & Assert
    final List<String> extractedSids =
        given()
            .header("Authorization", "Bearer " + ownerSid)
            .queryParam("accountSid", account.getSid().toString())
            .when()
            .get("/categories")
            .then()
            .statusCode(200)
            .extract()
            .path("sid");

    assertThat(extractedSids).doesNotContainAnyElementsOf(internalCategorySids);
  }

  @Test
  void shouldFilterOutInternalCategoriesWhenAccountIsNotSpecified() {
    // Arrange
    final List<String> internalCategorySids = SystemCategory.allSids().stream()
        .map(UUID::toString)
        .toList();

    // Act & Assert
    final List<String> extractedSids =
        given()
            .header("Authorization", "Bearer " + ownerSid)
            .when()
            .get("/categories")
            .then()
            .statusCode(200)
            .extract()
            .path("sid");

    assertThat(extractedSids).doesNotContainAnyElementsOf(internalCategorySids);
  }

  @Test
  void shouldFilterByDisplayNameCaseInsensitively() {
    // Act & Assert
    final List<String> extractedSids =
        given()
            .header("Authorization", "Bearer " + ownerSid)
            .queryParam("accountSid", account.getSid().toString())
            .queryParam("displayName", "chi")
            .when()
            .get("/categories")
            .then()
            .statusCode(200)
            .extract()
            .path("sid");

    assertThat(extractedSids).contains(accountCategory.getSid().toString());
    assertThat(extractedSids).doesNotContain(parentCategory.getSid().toString(), incomeCategory.getSid().toString());
  }

  @Test
  void shouldFilterByParentSid() {
    // Act & Assert
    final List<String> extractedSids =
        given()
            .header("Authorization", "Bearer " + ownerSid)
            .queryParam("accountSid", account.getSid().toString())
            .queryParam("parentSid", parentCategory.getSid().toString())
            .when()
            .get("/categories")
            .then()
            .statusCode(200)
            .extract()
            .path("sid");

    assertThat(extractedSids).contains(accountCategory.getSid().toString());
    assertThat(extractedSids).doesNotContain(parentCategory.getSid().toString(), incomeCategory.getSid().toString());
  }

  @Test
  void shouldFilterByTransactionType() {
    // Act & Assert
    final List<String> extractedSids =
        given()
            .header("Authorization", "Bearer " + ownerSid)
            .queryParam("accountSid", account.getSid().toString())
            .queryParam("type", TransactionType.INCOME.name())
            .when()
            .get("/categories")
            .then()
            .statusCode(200)
            .extract()
            .path("sid");

    assertThat(extractedSids).contains(incomeCategory.getSid().toString());
    assertThat(extractedSids).doesNotContain(parentCategory.getSid().toString(), accountCategory.getSid().toString());
  }

  @Test
  void shouldReturnEnrichedResponsePayload() {
    // Act & Assert
    given()
        .header("Authorization", "Bearer " + ownerSid)
        .queryParam("accountSid", account.getSid().toString())
        .queryParam("parentSid", parentCategory.getSid().toString())
        .when()
        .get("/categories")
        .then()
        .statusCode(200)
        .body("[0].sid", equalTo(accountCategory.getSid().toString()))
        .body("[0].displayName", equalTo("Child"))
        .body("[0].type", equalTo(TransactionType.EXPENSE.name()))
        .body("[0].isSystem", equalTo(false))
        .body("[0].parentSid", equalTo(parentCategory.getSid().toString()))
        .body("[0].accountSid", equalTo(account.getSid().toString()));
  }

  @Test
  void shouldReturnForbiddenWhenUserIsNotMemberOfAccount() {
    // Arrange
    final UUID nonMemberSid = UUID.randomUUID();

    // Act & Assert
    given()
        .header("Authorization", "Bearer " + nonMemberSid)
        .queryParam("accountSid", account.getSid().toString())
        .when()
        .get("/categories")
        .then()
        .statusCode(403);
  }

}
