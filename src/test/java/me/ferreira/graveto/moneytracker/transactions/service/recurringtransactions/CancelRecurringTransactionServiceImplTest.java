package me.ferreira.graveto.moneytracker.transactions.service.recurringtransactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsOnAccountException;
import me.ferreira.graveto.common.web.exception.moneytracker.RecurringTransactionNotFoundException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction.RecurringTransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.CancelRecurringTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.RecurringTransactionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CancelRecurringTransactionServiceImplTest {

  @InjectMocks
  private RecurringTransactionServiceImpl recurringTransactionService;
  @Mock
  private CategoryService categoryService;
  @Mock
  private AccountService accountService;
  @Mock
  private RecurringTransactionRepository recurringTransactionRepository;

  @Test
  void shouldThrowWhenRecurringTransactionNotFound() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();

    when(recurringTransactionRepository.findBySid(rtSid)).thenReturn(Optional.empty());

    final CancelRecurringTransactionCommand command = buildCommand(userSid, rtSid);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransactionService.cancelRecurringTransaction(command))
        .isInstanceOf(RecurringTransactionNotFoundException.class);
  }

  @Test
  void shouldThrowWhenUserLacksPermission() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID otherUserSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = buildAccount(accountSid, userSid);
    final RecurringTransaction existingRt = buildExistingRecurringTransaction(rtSid, account, null);

    final CancelRecurringTransactionCommand command = buildCommand(otherUserSid, existingRt.getSid());

    when(recurringTransactionRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));

    // Act & Assert
    assertThatThrownBy(() -> recurringTransactionService.cancelRecurringTransaction(command))
        .isInstanceOf(InsufficientPermissionsOnAccountException.class);
  }

  @Test
  void shouldCancelRecurringTransaction() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = buildAccount(accountSid, userSid);
    final LocalDate originalEndDate = LocalDate.of(2048, 1, 20);
    final RecurringTransaction existingRt = buildExistingRecurringTransaction(rtSid, account, originalEndDate);

    final CancelRecurringTransactionCommand command = buildCommand(userSid, existingRt.getSid());

    when(recurringTransactionRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));
    when(recurringTransactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final RecurringTransaction result =
        recurringTransactionService.cancelRecurringTransaction(command);

    // Assert
    assertThat(result.getStatus()).isEqualTo(RecurringOperationStatus.CANCELED);
    assertThat(result.getEndDate()).isNotEqualTo(originalEndDate);
    verify(recurringTransactionRepository).save(existingRt);
  }

  private static CancelRecurringTransactionCommand buildCommand(final UUID userSid, final UUID sid) {
    return new CancelRecurringTransactionCommand(
        userSid, sid);
  }

  private static Account buildAccount(final UUID accountSid, final UUID userSid) {
    final Account account = new Account();
    account.setSid(accountSid);
    account.setBaseCurrency(Currency.EUR);
    account.setStatus(AccountStatus.ACTIVE);
    account.setBalance(BigDecimal.ZERO);

    final AccountMembership membership = new AccountMembership();
    membership.setUserSid(userSid);
    membership.setRole(MembershipRole.OWNER);
    account.getMemberships().add(membership);

    return account;
  }

  private static RecurringTransaction buildExistingRecurringTransaction(final UUID rtSid, final Account account,
                                                                        final LocalDate endDate) {
    final Category category = new Category();
    category.setSid(UUID.randomUUID());
    category.setDisplayName("Insurance");

    final RecurringTransaction rt = new RecurringTransaction();
    rt.setSid(rtSid);
    rt.setAccount(account);
    rt.setEndDate(endDate);
    return rt;
  }

}
