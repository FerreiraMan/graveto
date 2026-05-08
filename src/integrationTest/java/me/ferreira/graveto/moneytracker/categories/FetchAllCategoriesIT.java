package me.ferreira.graveto.moneytracker.categories;

import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.utils.CategoryTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Sql(scripts = {"/moneytracker/sql/delete_specific_categories.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FetchAllCategoriesIT extends MoneyTrackerBaseIntegrationTest {

    private UUID ownerSid;
    private Category parentCategory;
    private Category userCategory;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setupTestBaseline() {
        this.ownerSid = UUID.randomUUID();

        this.parentCategory = CategoryTestFactory.createCategory("Parent", null, null, false);
        this.userCategory = CategoryTestFactory.createCategory("Child", ownerSid, parentCategory, false);

        categoryRepository.saveAll(List.of(parentCategory, userCategory));
    }

    @Test
    void shouldReturnAllCategoriesFromSystemAndUser() {
        // Act & Assert
        final List<String> extractedSids =
                given().
                        header("X-User-Sid", ownerSid).
                when().
                        get("/categories").
                then().
                        statusCode(200).
                        extract().
                        path("sid");

        assertThat(extractedSids).contains(
                parentCategory.getSid().toString(),
                userCategory.getSid().toString()
        );
    }

    @Test
    void shouldReturnAllCategoriesFromSystemAndNotUserSpecific() {
        // Act & Assert
        final List<String> extractedSids =
                given().
                        header("X-User-Sid", UUID.randomUUID()).
                when().
                        get("/categories").
                then().
                        statusCode(200).
                        extract().
                        path("sid");

        assertThat(extractedSids).contains(parentCategory.getSid().toString());
        assertThat(extractedSids).doesNotContain(userCategory.getSid().toString());
    }

    @Test
    void shouldFilterOutInternalCategories() {
        // Arrange
        final List<String> internalCategorySids = SystemCategory.allSids().stream()
                .map(UUID::toString)
                .toList();

        // Act & Assert
        final List<String> extractedSids =
                given().
                        header("X-User-Sid", UUID.randomUUID()).
                when().
                        get("/categories").
                then().
                        statusCode(200).
                        extract().
                        path("sid");

        assertThat(extractedSids).doesNotContainAnyElementsOf(internalCategorySids);
    }

}
