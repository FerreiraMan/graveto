package me.ferreira.graveto.moneytracker.categories;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.utils.AccountTestFactory;
import me.ferreira.graveto.moneytracker.utils.CategoryTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {
    "/moneytracker/sql/delete_specific_categories.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FetchAllCategoriesIT extends MoneyTrackerBaseIntegrationTest {

  final UUID ownerSid = UUID.randomUUID();
  private Account account;
  private Category parentCategory;
  private Category accountCategory;

  @Autowired
  private CategoryRepository categoryRepository;
  @Autowired
  private AccountRepository accountRepository;

  @BeforeEach
  void setupTestBaseline() {

    this.account = AccountTestFactory.createAccountWithOwner(ownerSid, "", BigDecimal.ONE);
    this.parentCategory = CategoryTestFactory.createCategory("Parent", null, null, false);
    this.accountCategory = CategoryTestFactory.createCategory("Child", account.getSid(), parentCategory, false);

    accountRepository.save(account);
    categoryRepository.saveAll(List.of(parentCategory, accountCategory));
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
        accountCategory.getSid().toString()
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
    assertThat(extractedSids).doesNotContain(accountCategory.getSid().toString());
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

}
