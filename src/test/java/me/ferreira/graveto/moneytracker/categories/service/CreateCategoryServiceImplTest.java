package me.ferreira.graveto.moneytracker.categories.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import me.ferreira.graveto.common.web.exception.ApplicationException;
import me.ferreira.graveto.common.web.exception.moneytracker.CategoryAlreadyExistsException;
import me.ferreira.graveto.common.web.exception.moneytracker.CategoryNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.IllegalCategoryHierarchyException;
import me.ferreira.graveto.common.web.exception.moneytracker.MaxCategoryDepthExceededException;
import me.ferreira.graveto.common.web.exception.moneytracker.UserNotMemberOfAccountException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.categories.service.command.CreateCategoryCommand;
import me.ferreira.graveto.moneytracker.categories.service.impl.CategoryServiceImpl;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import me.ferreira.graveto.moneytracker.utils.CategoryUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class CreateCategoryServiceImplTest {

  @InjectMocks
  private CategoryServiceImpl service;
  @Mock
  private CategoryRepository categoryRepository;
  @Mock
  private AccountService accountService;

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

  @Test
  void shouldThrowIfInternalCategoryDoesNotExistOnSystemCategories() {
    // Arrange
    final UUID randomSid = UUID.randomUUID();

    // Act & Assert
    assertThatThrownBy(() -> {
      service.fetchInternalCategory(randomSid);
    }).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Requested SID is not a valid Internal Category.");
  }

  @Test
  void shouldThrowIfInternalCategoryDoesNotExistOnDatabase() {
    // Arrange
    final Category expectedCategory = CategoryUtils.createInitialBalanceCategory();
    when(categoryRepository.findBySid(SystemCategory.INITIAL_BALANCE.getSid())).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> {
      service.fetchInternalCategory(expectedCategory.getSid());
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("Internal Category is missing from the database.");
  }

  @Test
  void shouldReturnInternalCategory() {
    // Arrange
    final Category expectedCategory = CategoryUtils.createInitialBalanceCategory();
    when(categoryRepository.findBySid(SystemCategory.INITIAL_BALANCE.getSid())).thenReturn(
        Optional.of(expectedCategory));

    // Act
    final Category fetchedCategory = service.fetchInternalCategory(SystemCategory.INITIAL_BALANCE.getSid());

    // Assert
    assertThat(fetchedCategory)
        .isNotNull()
        .usingRecursiveComparison()
        .isEqualTo(expectedCategory);

    verify(categoryRepository, times(1)).findBySid(SystemCategory.INITIAL_BALANCE.getSid());
  }

  @Test
  void shouldCreateNewCategory() {
    // Arrange
    final String expectedCategoryName = "Video games";
    final String sanitizedName = "VIDEO_GAMES";
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID parentSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);
    final CreateCategoryCommand command = new CreateCategoryCommand(
        userSid,
        expectedCategoryName,
        accountSid,
        parentSid,
        TransactionType.EXPENSE
    );
    final Category parentCategory =
        CategoryUtils.createCategory("Leisure", accountSid, null, false, TransactionType.EXPENSE);

    when(categoryRepository.existsByNameForAccountOrSystem(any(), any())).thenReturn(false);
    when(categoryRepository.findBySid(parentSid)).thenReturn(Optional.of(parentCategory));
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(categoryRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final Category createdCategory = service.createCategory(command);

    // Assert
    final ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
    verify(categoryRepository).save(captor.capture());

    final Category savedCategory = captor.getValue();

    assertThat(createdCategory.getName()).isEqualTo(sanitizedName);
    assertThat(createdCategory.getAccountSid()).isEqualTo(accountSid);
    assertThat(createdCategory.getDisplayName()).isEqualTo(expectedCategoryName);
    assertThat(createdCategory.getParent()).isEqualTo(parentCategory);
    assertThat(createdCategory.getSid()).isEqualTo(savedCategory.getSid());
  }

  @Test
  void shouldCreateCategoryWithDefaultCategoryAsParent() {
    // Arrange
    final String expectedCategoryName = "Video games";
    final String sanitizedName = "VIDEO_GAMES";
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);
    final CreateCategoryCommand command = new CreateCategoryCommand(
        userSid,
        expectedCategoryName,
        accountSid,
        null,
        TransactionType.EXPENSE
    );

    when(categoryRepository.existsByNameForAccountOrSystem(any(), any())).thenReturn(false);
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(categoryRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final Category createdCategory = service.createCategory(command);

    // Assert
    final ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
    verify(categoryRepository).save(captor.capture());

    final Category savedCategory = captor.getValue();

    assertThat(createdCategory.getName()).isEqualTo(sanitizedName);
    assertThat(createdCategory.getAccountSid()).isEqualTo(accountSid);
    assertThat(createdCategory.getDisplayName()).isEqualTo(expectedCategoryName);
    assertThat(createdCategory.getParent()).isNull();
    assertThat(createdCategory.getSid()).isEqualTo(savedCategory.getSid());
  }

  @ParameterizedTest
  @MethodSource("validCategoryNameRequest")
  void shouldSanitizeValidCategoryNames(final String receivedName, final String expectedSanitizedName) {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);
    final CreateCategoryCommand command =
        new CreateCategoryCommand(userSid, receivedName, accountSid, null, TransactionType.EXPENSE);

    // Act & Assert
    when(categoryRepository.existsByNameForAccountOrSystem(anyString(), eq(accountSid))).thenReturn(false);
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);

    service.createCategory(command);

    final ArgumentCaptor<String> sanitizedNameCaptor = ArgumentCaptor.forClass(String.class);
    verify(categoryRepository).existsByNameForAccountOrSystem(sanitizedNameCaptor.capture(), eq(accountSid));
    assertEquals(expectedSanitizedName, sanitizedNameCaptor.getValue());
  }

  @ParameterizedTest
  @MethodSource("invalidCategoryNameRequest")
  void shouldThrowOnBlankCategoryNames(final String receivedName, final String expectedSanitizedName) {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);
    final CreateCategoryCommand command =
        new CreateCategoryCommand(userSid, receivedName, accountSid, null, TransactionType.EXPENSE);
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);

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
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);
    final CreateCategoryCommand command =
        new CreateCategoryCommand(userSid, name, accountSid, UUID.randomUUID(),
            TransactionType.EXPENSE);

    when(categoryRepository.existsByNameForAccountOrSystem(any(), any())).thenReturn(true);
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.createCategory(command);
    }).isInstanceOf(CategoryAlreadyExistsException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          Assertions.assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
          Assertions.assertThat(ae.getSafeMessage()).isEqualTo("A category with this name already exists.");
        });
  }

  @Test
  void shouldThrowIfParentCategoryIsNotFound() {
    // Arrange
    final String name = "Videogames";
    final UUID parentSid = UUID.randomUUID();
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);
    final CreateCategoryCommand command =
        new CreateCategoryCommand(userSid, name, accountSid, parentSid, TransactionType.EXPENSE);

    when(categoryRepository.existsByNameForAccountOrSystem(any(), any())).thenReturn(false);
    when(categoryRepository.findBySid(parentSid)).thenReturn(Optional.empty());
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.createCategory(command);
    }).isInstanceOf(CategoryNotFoundException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          Assertions.assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
          Assertions.assertThat(ae.getSafeMessage()).isEqualTo("The category account was not found.");
        });
  }

  @Test
  void shouldThrowIfParentCategoryIsAtMaxDepth() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID otherAccountSid = UUID.randomUUID();
    final String name = "Videogames";
    final UUID parentSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);
    final Category rootCategory =
        CategoryUtils.createCategory("Transportation", null, null, false, TransactionType.EXPENSE);
    final Category grandparentCategory =
        CategoryUtils.createCategory("Fuel", null, rootCategory, false, TransactionType.EXPENSE);
    final Category parentCategory =
        CategoryUtils.createCategory("Diesel", otherAccountSid, grandparentCategory, false,
            TransactionType.EXPENSE);
    final CreateCategoryCommand command =
        new CreateCategoryCommand(userSid, name, accountSid, parentSid, TransactionType.EXPENSE);

    when(categoryRepository.existsByNameForAccountOrSystem(any(), any())).thenReturn(false);
    when(categoryRepository.findBySid(parentSid)).thenReturn(Optional.of(parentCategory));
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.createCategory(command);
    }).isInstanceOf(MaxCategoryDepthExceededException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          Assertions.assertThat(ae.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
          Assertions.assertThat(ae.getSafeMessage())
              .isEqualTo("Category depth must be kept at 3 levels maximum.");
        });
  }

  @Test
  void shouldCreateCategoryWhenParentIsAtSecondLevel() {
    // Arrange
    final String expectedCategoryName = "Diesel Top Up";
    final String sanitizedName = "DIESEL_TOP_UP";
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID parentSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);
    final Category rootCategory =
        CategoryUtils.createCategory("Transportation", null, null, false, TransactionType.EXPENSE);
    final Category parentCategory =
        CategoryUtils.createCategory("Fuel", null, rootCategory, false, TransactionType.EXPENSE);
    final CreateCategoryCommand command = new CreateCategoryCommand(
        userSid,
        expectedCategoryName,
        accountSid,
        parentSid,
        TransactionType.EXPENSE
    );

    when(categoryRepository.existsByNameForAccountOrSystem(any(), any())).thenReturn(false);
    when(categoryRepository.findBySid(parentSid)).thenReturn(Optional.of(parentCategory));
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(categoryRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final Category createdCategory = service.createCategory(command);

    // Assert
    assertThat(createdCategory.getName()).isEqualTo(sanitizedName);
    assertThat(createdCategory.getParent()).isEqualTo(parentCategory);
  }

  @Test
  void shouldThrowIfParentCategoryIsOwnedByOtherAccount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID otherAccountSid = UUID.randomUUID();
    final String name = "Videogames";
    final UUID parentSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);
    final Category parentCategory =
        CategoryUtils.createCategory("Leisure", otherAccountSid, null, false, TransactionType.EXPENSE);
    final CreateCategoryCommand command =
        new CreateCategoryCommand(userSid, name, accountSid, parentSid, TransactionType.EXPENSE);

    when(categoryRepository.existsByNameForAccountOrSystem(any(), any())).thenReturn(false);
    when(categoryRepository.findBySid(parentSid)).thenReturn(Optional.of(parentCategory));
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.createCategory(command);
    }).isInstanceOf(CategoryNotFoundException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          Assertions.assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
          Assertions.assertThat(ae.getSafeMessage()).isEqualTo("The category account was not found.");
        });
  }

  @Test
  void shouldThrowIfTransactionTypeDoesNotMatchParentTransactionType() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final String name = "Videogames";
    final UUID parentSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);
    final Category parentCategory =
        CategoryUtils.createCategory("Leisure", accountSid, null, false, TransactionType.EXPENSE);
    final CreateCategoryCommand command =
        new CreateCategoryCommand(userSid, name, accountSid, parentSid, TransactionType.INCOME);

    when(categoryRepository.existsByNameForAccountOrSystem(any(), any())).thenReturn(false);
    when(categoryRepository.findBySid(parentSid)).thenReturn(Optional.of(parentCategory));
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.createCategory(command);
    }).isInstanceOf(IllegalCategoryHierarchyException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          Assertions.assertThat(ae.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
          Assertions.assertThat(ae.getSafeMessage())
              .isEqualTo("Category transaction type must match the parent category's transaction type.");
        });
  }

  @Test
  void shouldThrowIfUserIsNotMemberOfAccount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, UUID.randomUUID(), MembershipRole.OWNER);
    final CreateCategoryCommand command =
        new CreateCategoryCommand(userSid, "Leisure", accountSid, null, TransactionType.EXPENSE);
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.createCategory(command);
    }).isInstanceOf(UserNotMemberOfAccountException.class)
        .hasMessage("The user is not a member of this account.");
  }

}
