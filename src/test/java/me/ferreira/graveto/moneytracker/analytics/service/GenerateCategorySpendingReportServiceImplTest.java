package me.ferreira.graveto.moneytracker.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.analytics.service.command.CategorySpendingCommand;
import me.ferreira.graveto.moneytracker.analytics.service.impl.AnalyticServiceImpl;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CategorySpendingResult;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.categories.service.command.FetchAllCategoriesCommand;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.CategoryAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.GenerateCategoryAggregateCommand;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GenerateCategorySpendingReportServiceImplTest {

  @InjectMocks
  private AnalyticServiceImpl service;
  @Mock
  private AccountService accountService;
  @Mock
  private TransactionService transactionService;
  @Mock
  private CategoryService categoryService;

  @Test
  void shouldThrowIfAccountIsNotFoundDuringCategorySpendingGeneration() {
    // Arrange
    final UUID accountSid = UUID.randomUUID();

    when(accountService.fetchAccountEntity(any())).thenThrow(new AccountNotFoundException(accountSid));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.generateCategorySpendingReport(mock(CategorySpendingCommand.class));
    }).isInstanceOf(AccountNotFoundException.class)
        .hasMessage("Account with SID [" + accountSid + "] was not found or you do not have permission to view it.");
  }

  @Test
  void shouldThrowIfUserIsNotAuthorizedToRequestCategorySpendingGeneration() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, null);
    final CategorySpendingCommand command = mock(CategorySpendingCommand.class);

    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(command.userSid()).thenReturn(userSid);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.generateCategorySpendingReport(command);
    }).isInstanceOf(InsufficientPermissionsException.class)
        .hasMessage("User does not have the required role to request category spending report for this account.");
  }

  @Test
  void shouldMapFlatProjectionsToInfiniteDepthCategoryTree() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final int year = 2026;

    final CategorySpendingCommand command = new CategorySpendingCommand(userSid, account.getSid(), year);
    final GenerateCategoryAggregateCommand aggregateCommand =
        new GenerateCategoryAggregateCommand(year, account.getSid());

    // Level 1: Housing
    final Category housing = new Category();
    housing.setSid(UUID.randomUUID());
    housing.setDisplayName("Housing");

    // Level 2: Utilities (Child of Housing)
    final Category utilities = new Category();
    utilities.setSid(UUID.randomUUID());
    utilities.setDisplayName("Utilities");
    utilities.setParent(housing);

    // Level 3: Electricity (Child of Utilities)
    final Category electricity = new Category();
    electricity.setSid(UUID.randomUUID());
    electricity.setDisplayName("Electricity");
    electricity.setParent(utilities);

    // Unused Category (Should be filtered out because it has 0€)
    final Category leisure = new Category();
    leisure.setSid(UUID.randomUUID());
    leisure.setDisplayName("Leisure");

    final List<Category> userCategories = List.of(housing, utilities, electricity, leisure);

    final List<CategoryAggregateProjection> projections = List.of(
        new MockCategoryProjection(1, electricity.getSid(), new BigDecimal("50.00")),
        new MockCategoryProjection(1, utilities.getSid(), new BigDecimal("20.00")),
        new MockCategoryProjection(2, housing.getSid(), new BigDecimal("1000.00"))
    );

    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(categoryService.fetchAllCategories(new FetchAllCategoriesCommand(userSid, account.getSid()))).thenReturn(
        userCategories);
    when(transactionService.generateCategoryAggregates(aggregateCommand)).thenReturn(projections);

    // Act
    final CategorySpendingResult result = service.generateCategorySpendingReport(command);

    // Assert
    assertThat(result.year()).isEqualTo(year);
    assertThat(result.categories()).hasSize(1);

    // Level 1 (Housing)
    final CategorySpendingResult.CategoryAggregate housingAggr = result.categories().get(0);
    assertThat(housingAggr.categoryName()).isEqualTo("Housing");
    assertThat(housingAggr.yearlyTotal()).isEqualByComparingTo("1070.00");
    assertThat(housingAggr.monthlyTotals().get(1)).isEqualByComparingTo("70.00");
    assertThat(housingAggr.monthlyTotals().get(2)).isEqualByComparingTo("1000.00");
    assertThat(housingAggr.monthlyTotals().get(3)).isEqualByComparingTo("0.00");
    assertThat(housingAggr.childCategories()).hasSize(1);

    // Level 2 (Utilities)
    final CategorySpendingResult.CategoryAggregate utilitiesAggr = housingAggr.childCategories().get(0);
    assertThat(utilitiesAggr.categoryName()).isEqualTo("Utilities");
    assertThat(utilitiesAggr.yearlyTotal()).isEqualByComparingTo("70.00");
    assertThat(utilitiesAggr.monthlyTotals().get(1)).isEqualByComparingTo("70.00");
    assertThat(utilitiesAggr.childCategories()).hasSize(1);

    // Level 3 (Electricity)
    final CategorySpendingResult.CategoryAggregate electricityAggr = utilitiesAggr.childCategories().get(0);
    assertThat(electricityAggr.categoryName()).isEqualTo("Electricity");
    assertThat(electricityAggr.yearlyTotal()).isEqualByComparingTo("50.00");
    assertThat(electricityAggr.monthlyTotals().get(1)).isEqualByComparingTo("50.00");
    assertThat(electricityAggr.childCategories()).isEmpty();
  }

  private record MockCategoryProjection(int month, UUID categorySid, BigDecimal amount)
      implements CategoryAggregateProjection {
    @Override
    public int getMonth() {
      return month;
    }

    @Override
    public UUID getCategorySid() {
      return categorySid;
    }

    @Override
    public BigDecimal getTotalAmount() {
      return amount;
    }
  }

}
