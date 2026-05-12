package me.ferreira.graveto.moneytracker.categories;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.restassured.http.ContentType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.categories.web.dto.request.CreateCategoryRequestDto;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.utils.CategoryTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {
    "/moneytracker/sql/delete_specific_categories.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CreateCategoryIT extends MoneyTrackerBaseIntegrationTest {

  private Category parentCategory;

  @Autowired
  private CategoryRepository categoryRepository;

  @BeforeEach
  void setupTestBaseline() {

    this.parentCategory = CategoryTestFactory.createCategory("Parent", null, null, false);
    categoryRepository.saveAll(List.of(parentCategory));
  }

  @Test
  void shouldCreateCategory() {
    // Arrange
    final String expectedCategoryName = "Oil Gas";
    final String expectedSanitizedName = "OIL_GAS";
    final CreateCategoryRequestDto requestDto = new CreateCategoryRequestDto(
        expectedCategoryName, parentCategory.getSid(), TransactionType.EXPENSE
    );
    final UUID userSid = UUID.randomUUID();

    // Act
    final String categorySid =
        given()
            .header("Authorization", "Bearer " + userSid)
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
    assertThat(savedCategory.getUserSid()).isEqualTo(userSid);
    assertThat(savedCategory.getParent().getSid()).isEqualTo(this.parentCategory.getSid());
    assertThat(savedCategory.getTransactionType()).isEqualTo(TransactionType.EXPENSE);
  }

}
