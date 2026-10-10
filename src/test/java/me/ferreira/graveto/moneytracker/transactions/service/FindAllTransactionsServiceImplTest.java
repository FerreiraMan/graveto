package me.ferreira.graveto.moneytracker.transactions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.ApplicationException;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsOnAccountException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionSearchCriteria;
import me.ferreira.graveto.moneytracker.transactions.service.command.FindAllTransactionsCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.TransactionServiceImpl;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class FindAllTransactionsServiceImplTest {

  @InjectMocks
  private TransactionServiceImpl service;
  @Mock
  private CategoryService categoryService;
  @Mock
  private AccountService accountService;
  @Mock
  private TransactionRepository transactionRepository;

  @Test
  void shouldThrowIfAccountIsNotFoundDuringFindAllTransactions() {
    // Arrange
    when(accountService.fetchAccountEntity(any())).thenThrow(new AccountNotFoundException("loggableMessage"));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.findAll(mock(FindAllTransactionsCommand.class));
    }).isInstanceOf(AccountNotFoundException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
          assertThat(ae.getSafeMessage()).isEqualTo(
              "The specified account is no longer available. " +
                  "It may have been removed, or you may not have access to it.");
        });
  }

  @Test
  void shouldThrowIfUserIsNotMemberOfTheAccountAndNeverTouchTheCategoriesOrTransactions() {
    // Arrange
    final UUID accountSid = UUID.randomUUID();
    final UUID strangerSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, UUID.randomUUID(), MembershipRole.OWNER);

    final FindAllTransactionsCommand command = new FindAllTransactionsCommand(
        strangerSid, accountSid, UUID.randomUUID(), null, null, null, null, Pageable.ofSize(2));

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);

    // Act & Assert
    assertThatThrownBy(() -> service.findAll(command))
        .isInstanceOf(InsufficientPermissionsOnAccountException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          assertThat(ae.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(ae.getSafeMessage()).isEqualTo("You do not have the required role to perform this action.");
        });

    verifyNoInteractions(categoryService, transactionRepository);
  }

  @ParameterizedTest
  @EnumSource(value = MembershipRole.class, names = {"OWNER", "CONTRIBUTOR"})
  void shouldAllowEveryMemberRoleThatCanReadTransactions(final MembershipRole role) {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, userSid, role);

    final FindAllTransactionsCommand command = new FindAllTransactionsCommand(
        userSid, accountSid, null, null, null, null, null, Pageable.ofSize(2));

    final Page<Transaction> expectedPage = mock(Page.class);
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(transactionRepository.findAll(any(TransactionSearchCriteria.class))).thenReturn(expectedPage);

    // Act
    final Page<Transaction> actualPage = service.findAll(command);

    // Assert
    assertThat(actualPage).isSameAs(expectedPage);
  }

  @Test
  void shouldReturnAllTransactionsPageable() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID categorySid = UUID.randomUUID();
    final LocalDate startDate = LocalDate.of(2025, 1, 1);
    final LocalDate endDate = LocalDate.of(2025, 12, 1);
    final Pageable pageable = Pageable.ofSize(2);

    final FindAllTransactionsCommand command = new FindAllTransactionsCommand(
        userSid,
        accountSid,
        categorySid,
        startDate,
        endDate,
        TransactionType.EXPENSE,
        TransactionStatus.ACTIVE,
        pageable
    );

    final Account account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);
    final Page<Transaction> expectedPage = mock(Page.class);

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(categoryService.fetchCategoryAndAllDescendants(categorySid, accountSid))
        .thenReturn(List.of(categoryWithId(10L), categoryWithId(11L), categoryWithId(12L)));
    when(transactionRepository.findAll(any(TransactionSearchCriteria.class))).thenReturn(expectedPage);

    // Act
    final Page<Transaction> actualPage = service.findAll(command);

    // Assert
    assertThat(actualPage).isSameAs(expectedPage);

    final ArgumentCaptor<TransactionSearchCriteria> criteriaCaptor =
        ArgumentCaptor.forClass(TransactionSearchCriteria.class);
    verify(transactionRepository).findAll(criteriaCaptor.capture());

    final TransactionSearchCriteria criteria = criteriaCaptor.getValue();
    assertThat(criteria.accountSid()).isEqualTo(accountSid);
    assertThat(criteria.categoryIds()).containsExactlyInAnyOrder(10L, 11L, 12L);
    assertThat(criteria.startDate()).isEqualTo(startDate);
    assertThat(criteria.endDate()).isEqualTo(endDate);
    assertThat(criteria.type()).isEqualTo(TransactionType.EXPENSE);
    assertThat(criteria.status()).isEqualTo(TransactionStatus.ACTIVE);
    assertThat(criteria.pageable()).isSameAs(pageable);
  }

  @Test
  void shouldNotResolveCategoriesAndSendNullCategoryIdsWhenNoCategoryIsRequested() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);

    final FindAllTransactionsCommand command = new FindAllTransactionsCommand(
        userSid, accountSid, null, null, null, null, null, Pageable.ofSize(2));

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(transactionRepository.findAll(any(TransactionSearchCriteria.class))).thenReturn(mock(Page.class));

    // Act
    service.findAll(command);

    // Assert
    verifyNoInteractions(categoryService);

    final ArgumentCaptor<TransactionSearchCriteria> criteriaCaptor =
        ArgumentCaptor.forClass(TransactionSearchCriteria.class);
    verify(transactionRepository).findAll(criteriaCaptor.capture());
    assertThat(criteriaCaptor.getValue().categoryIds()).isNull();
  }

  @Test
  void shouldSendEmptyCategoryIdsWhenTheRequestedCategoryResolvesToNothing() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID unknownCategorySid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);

    final FindAllTransactionsCommand command = new FindAllTransactionsCommand(
        userSid, accountSid, unknownCategorySid, null, null, null, null, Pageable.ofSize(2));

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(categoryService.fetchCategoryAndAllDescendants(unknownCategorySid, accountSid)).thenReturn(List.of());
    when(transactionRepository.findAll(any(TransactionSearchCriteria.class))).thenReturn(mock(Page.class));

    // Act
    service.findAll(command);

    // Assert
    final ArgumentCaptor<TransactionSearchCriteria> criteriaCaptor =
        ArgumentCaptor.forClass(TransactionSearchCriteria.class);
    verify(transactionRepository).findAll(criteriaCaptor.capture());
    assertThat(criteriaCaptor.getValue().categoryIds()).isNotNull().isEmpty();
  }

  private static Category categoryWithId(final Long id) {
    final Category category = new Category();
    category.setId(id);
    return category;
  }

}
