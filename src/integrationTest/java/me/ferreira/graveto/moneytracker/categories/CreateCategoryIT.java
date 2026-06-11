package me.ferreira.graveto.moneytracker.categories;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.categories.web.dto.request.CreateCategoryRequestDto;
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
public class CreateCategoryIT extends MoneyTrackerBaseIntegrationTest {

  final UUID ownerSid = UUID.randomUUID();
  private Category parentCategory;
  private Account account;

  @Autowired
  private CategoryRepository categoryRepository;
  @Autowired
  private AccountRepository accountRepository;

  @BeforeEach
  void setupTestBaseline() {
    this.account = AccountTestFactory.createAccountWithOwner(ownerSid, "", BigDecimal.ONE);
    this.parentCategory = CategoryTestFactory.createCategory("Parent", null, null, false);
    categoryRepository.saveAll(List.of(parentCategory));
    accountRepository.save(account);
  }

  @Test
  void shouldCreateCategory() {
    // Arrange
    final String expectedCategoryName = "Oil Gas";
    final String expectedSanitizedName = "OIL_GAS";
    final CreateCategoryRequestDto requestDto = new CreateCategoryRequestDto(
        expectedCategoryName, account.getSid(), parentCategory.getSid(), TransactionType.EXPENSE
    );

    // Act
    final String categorySid =
        given()
            .header("Authorization", "Bearer " + ownerSid)
            .contentType(ContentType.JSON)
            .body(requestDto)
            .when()
            .post("/categories").then()
            .statusCode(201)
            .extract()
            .path("sid");

    // Assert
    final Optional<Category> categoryOptional = categoryRepository.findBySid(UUID.fromString(categorySid));
    assertThat(categoryOptional).isPresent();

    final Category savedCategory = categoryOptional.get();
    assertThat(savedCategory.getName()).isEqualTo(expectedSanitizedName);
    assertThat(savedCategory.getDisplayName()).isEqualTo(expectedCategoryName);
    assertThat(savedCategory.getAccountSid()).isEqualTo(account.getSid());
    assertThat(savedCategory.getParent().getSid()).isEqualTo(this.parentCategory.getSid());
    assertThat(savedCategory.getTransactionType()).isEqualTo(TransactionType.EXPENSE);
  }

}
