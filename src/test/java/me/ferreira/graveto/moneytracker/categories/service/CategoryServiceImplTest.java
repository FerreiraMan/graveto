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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(categoryRepository.findBySid(SystemCategory.INITIAL_BALANCE)).thenReturn(Optional.of(expectedCategory));

        // Act
        final Category fetchedCategory = service.getInitialBalanceCategory();

        // Assert
        assertThat(fetchedCategory)
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(expectedCategory);

        verify(categoryRepository, times(1)).findBySid(SystemCategory.INITIAL_BALANCE);
    }

}
