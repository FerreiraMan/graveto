package me.ferreira.graveto.moneytracker.categories.service;

import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.categories.service.impl.CategoryServiceImpl;
import me.ferreira.graveto.moneytracker.utils.CategoryUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceImplTest {

    @InjectMocks
    private CategoryServiceImpl service;
    @Mock
    private CategoryRepository categoryRepository;

    @Test
    void shouldReturnInitialBalanceCategory() {
        // Arrange
        final Category expectedCategory = CategoryUtils.createInitialBalanceCategory();
        when(categoryRepository.findBySid(SystemCategory.INITIAL_BALANCE.getSid())).thenReturn(Optional.of(expectedCategory));

        // Act
        final Category fetchedCategory = service.getInitialBalanceCategory();

        // Assert
        assertThat(fetchedCategory)
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(expectedCategory);

        verify(categoryRepository, times(1)).findBySid(SystemCategory.INITIAL_BALANCE.getSid());
    }

    @Test
    void shouldReturnAllCategories() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final Category expectedCategory = CategoryUtils.createCategory("Gas", null, null, false);
        when(categoryRepository.findAllByUserSid(userSid)).thenReturn(List.of(expectedCategory));

        // Act
        final List<Category> categoryList = service.fetchAllCategories(userSid);

        // Assert
        assertThat(categoryList)
                .isNotNull()
                .first()
                .usingRecursiveComparison()
                .isEqualTo(expectedCategory);

        verify(categoryRepository, times(1)).findAllByUserSid(userSid);
    }

    @Test
    void shouldThrowIfNoSystemCategoriesAreReturned() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final Category expectedCategory = CategoryUtils.createCategory("Gas", userSid, null, false);
        when(categoryRepository.findAllByUserSid(userSid)).thenReturn(List.of(expectedCategory));

        // Act & Assert
        assertThatThrownBy(() -> {
            service.fetchAllCategories(userSid);
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("CRITICAL: System categories are missing from the database.");
    }

}
