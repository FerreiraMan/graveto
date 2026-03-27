package me.ferreira.graveto.moneytracker.categories.service;

import me.ferreira.graveto.common.web.exception.moneytracker.CategoryAlreadyExistsException;
import me.ferreira.graveto.common.web.exception.moneytracker.CategoryNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.IllegalCategoryHierarchyException;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.categories.service.command.CreateCategoryCommand;
import me.ferreira.graveto.moneytracker.categories.service.impl.CategoryServiceImpl;
import me.ferreira.graveto.moneytracker.utils.CategoryUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void shouldCreateNewCategory() {
        // Arrange
        final String expectedCategoryName = "Video games";
        final String sanitizedName = "VIDEO_GAMES";
        final UUID userSid = UUID.randomUUID();
        final UUID parentSid = UUID.randomUUID();
        final CreateCategoryCommand command = new CreateCategoryCommand(
                userSid,
                expectedCategoryName,
                parentSid
        );
        final Category parentCategory = CategoryUtils.createCategory("Leisure", userSid, null, false);

        when(categoryRepository.existsByNameForUserOrSystem(any(), any())).thenReturn(false);
        when(categoryRepository.findBySid(parentSid)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // Act
        final Category createdCategory = service.createCategory(command);

        // Assert
        final ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());

        final Category savedCategory = captor.getValue();

        assertThat(createdCategory.getName()).isEqualTo(sanitizedName);
        assertThat(createdCategory.getDisplayName()).isEqualTo(expectedCategoryName);
        assertThat(createdCategory.getParent()).isEqualTo(parentCategory);
        assertThat(createdCategory.getSid()).isEqualTo(savedCategory.getSid());
    }

    @Test
    void shouldCreateCategoryWithSystemCategoryAsParent() {
        // Arrange
        final String expectedCategoryName = "Video games";
        final String sanitizedName = "VIDEO_GAMES";
        final UUID userSid = UUID.randomUUID();
        final CreateCategoryCommand command = new CreateCategoryCommand(
                userSid,
                expectedCategoryName,
                null
        );

        when(categoryRepository.existsByNameForUserOrSystem(any(), any())).thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // Act
        final Category createdCategory = service.createCategory(command);

        // Assert
        final ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());

        final Category savedCategory = captor.getValue();

        assertThat(createdCategory.getName()).isEqualTo(sanitizedName);
        assertThat(createdCategory.getDisplayName()).isEqualTo(expectedCategoryName);
        assertThat(createdCategory.getParent()).isNull();
        assertThat(createdCategory.getSid()).isEqualTo(savedCategory.getSid());
    }

    @ParameterizedTest
    @MethodSource("validCategoryNameRequest")
    void shouldSanitizeValidCategoryNames(final String receivedName, final String expectedSanitizedName) {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final CreateCategoryCommand command = new CreateCategoryCommand(userSid, receivedName, null);

        // Act & Assert
        when(categoryRepository.existsByNameForUserOrSystem(anyString(), eq(userSid))).thenReturn(false);

        service.createCategory(command);

        final ArgumentCaptor<String> sanitizedNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(categoryRepository).existsByNameForUserOrSystem(sanitizedNameCaptor.capture(), eq(userSid));
        assertEquals(expectedSanitizedName, sanitizedNameCaptor.getValue());
    }

    @ParameterizedTest
    @MethodSource("invalidCategoryNameRequest")
    void shouldThrowOnBlankCategoryNames(final String receivedName, final String expectedSanitizedName) {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final CreateCategoryCommand command = new CreateCategoryCommand(userSid, receivedName, null);

        // Act & Assert
        assertThatThrownBy(() -> {
            service.createCategory(command);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category name cannot be empty.");
    }

    @Test
    void shouldThrowIfCategoryAlreadyExists() {
        // Arrange
        final String name = "Videogames";
        final CreateCategoryCommand command = new CreateCategoryCommand(UUID.randomUUID(), name, UUID.randomUUID());

        when(categoryRepository.existsByNameForUserOrSystem(any(), any())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> {
            service.createCategory(command);
        }).isInstanceOf(CategoryAlreadyExistsException.class)
                .hasMessage("Category with name ["+ name + "] already exists.");
    }

    @Test
    void shouldThrowIfParentCategoryIsNotFound() {
        // Arrange
        final String name = "Videogames";
        final UUID parentSid = UUID.randomUUID();
        final CreateCategoryCommand command = new CreateCategoryCommand(UUID.randomUUID(), name, parentSid);

        when(categoryRepository.existsByNameForUserOrSystem(any(), any())).thenReturn(false);
        when(categoryRepository.findBySid(parentSid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> {
            service.createCategory(command);
        }).isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category with SID [" + parentSid + "] was not found.");
    }

    @Test
    void shouldThrowIfParentCategoryIsOwnedByOtherUser() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final UUID externalUser = UUID.randomUUID();
        final String name = "Videogames";
        final UUID parentSid = UUID.randomUUID();
        final Category parentCategory = CategoryUtils.createCategory("Leisure", externalUser, null, false);
        final CreateCategoryCommand command = new CreateCategoryCommand(userSid, name, parentSid);

        when(categoryRepository.existsByNameForUserOrSystem(any(), any())).thenReturn(false);
        when(categoryRepository.findBySid(parentSid)).thenReturn(Optional.of(parentCategory));

        // Act & Assert
        assertThatThrownBy(() -> {
            service.createCategory(command);
        }).isInstanceOf(IllegalCategoryHierarchyException.class)
                .hasMessage("Cannot use another user's category as a parent.");
    }

    private static Stream<Arguments> validCategoryNameRequest() {
        return Stream.of(
                Arguments.of("Oil and Gas", "OIL_AND_GAS"),
                Arguments.of("Oil and Gás", "OIL_AND_GAS"),
                Arguments.of("   Oil ãnd Gás  ", "OIL_AND_GAS"),
                Arguments.of(" Oîl    and   Gás ", "OIL_AND_GAS"),
                Arguments.of("OIL_AND_GAS", "OIL_AND_GAS"),
                Arguments.of("Level 1 Category", "LEVEL_1_CATEGORY"),
                Arguments.of("Groceries", "GROCERIES")
        );
    }

    private static Stream<Arguments> invalidCategoryNameRequest() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("  ", ""),
                Arguments.of(null, "")
        );
    }

}
