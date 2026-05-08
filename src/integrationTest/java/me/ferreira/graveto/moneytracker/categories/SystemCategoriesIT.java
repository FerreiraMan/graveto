package me.ferreira.graveto.moneytracker.categories;

import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemCategoriesIT extends MoneyTrackerBaseIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldHaveDefinedInternalSystemCategories() {
        // Arrange
        final List<UUID> definedSystemCategoriesSids = SystemCategory.allSids();

        // Act
        final List<Category> categoryList = categoryRepository.findByUserSidIsNull();

        // Assert
        final List<UUID> existingSystemCategoriesSids = categoryList.stream()
                .filter(Category::isInternal)
                .map(Category::getSid)
                .toList();

        assertThat(definedSystemCategoriesSids).hasSameElementsAs(existingSystemCategoriesSids);
    }

}
