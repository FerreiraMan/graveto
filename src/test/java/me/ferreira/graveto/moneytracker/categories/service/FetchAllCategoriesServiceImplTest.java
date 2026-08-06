package me.ferreira.graveto.moneytracker.categories.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.moneytracker.UserNotMemberOfAccountException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.categories.service.command.FindAllCategoriesCommand;
import me.ferreira.graveto.moneytracker.categories.service.impl.CategoryServiceImpl;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import me.ferreira.graveto.moneytracker.utils.CategoryUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FetchAllCategoriesServiceImplTest {

  @InjectMocks
  private CategoryServiceImpl service;
  @Mock
  private AccountService accountService;
  @Mock
  private CategoryRepository categoryRepository;

  @Test
  void shouldReturnOnlyDefaultCategoriesIfNotAccountIsSpecified() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Category expectedCategory = CategoryUtils.createCategory("Gas", null, null, false, TransactionType.EXPENSE);
    when(categoryRepository.findAll(new FindAllCategoriesCommand(userSid, null, null, null, null))).thenReturn(
        List.of(expectedCategory));

    // Act
    final List<Category> categoryList =
        service.fetchAllCategories(new FindAllCategoriesCommand(userSid, null, null, null, null));

    // Assert
    assertThat(categoryList)
        .isNotNull()
        .first()
        .usingRecursiveComparison()
        .isEqualTo(expectedCategory);

    verify(categoryRepository, times(1)).findAll(new FindAllCategoriesCommand(userSid, null, null, null, null));
  }

  @Test
  void shouldReturnDefaultAndCustomCategoriesIfAccountIsSpecified() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);
    final Category expectedCategory = CategoryUtils.createCategory("Gas", null, null, false, TransactionType.EXPENSE);
    when(categoryRepository.findAll(new FindAllCategoriesCommand(userSid, null, accountSid, null, null))).thenReturn(
        List.of(expectedCategory));
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);

    // Act
    final List<Category> categoryList =
        service.fetchAllCategories(new FindAllCategoriesCommand(userSid, null, account.getSid(), null, null));

    // Assert
    assertThat(categoryList)
        .isNotNull()
        .first()
        .usingRecursiveComparison()
        .isEqualTo(expectedCategory);

    verify(categoryRepository, times(1)).findAll(new FindAllCategoriesCommand(userSid, null, accountSid, null, null));
  }

  @Test
  void shouldThrowIfUserIsNotMemberOfAccount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, UUID.randomUUID(), MembershipRole.OWNER);
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.fetchAllCategories(new FindAllCategoriesCommand(userSid, null, account.getSid(), null, null));
    }).isInstanceOf(UserNotMemberOfAccountException.class)
        .hasMessage("The user is not a member of this account.");
  }

}
