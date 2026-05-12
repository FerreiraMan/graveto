package me.ferreira.graveto.moneytracker.transactions.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.event.AccountCreatedEvent;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import me.ferreira.graveto.moneytracker.utils.CategoryUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AccountCreationEventListenerTest {

  @InjectMocks
  private AccountCreatedEventListener listener;
  @Mock
  private CategoryService categoryService;
  @Mock
  private TransactionRepository transactionRepository;

  @Test
  void shouldCreateOpeningBalanceTransaction() {
    // Arrange
    final Account account = AccountUtils.createAccount(BigDecimal.TEN);
    final Category mockCategory = CategoryUtils.createInitialBalanceCategory();

    final AccountCreatedEvent event = new AccountCreatedEvent(account, BigDecimal.TEN);

    when(categoryService.fetchInternalCategory(any())).thenReturn(mockCategory);
    when(transactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    listener.onAccountCreation(event);

    // Assert
    final ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
    verify(transactionRepository).save(captor.capture());

    final Transaction savedTransaction = captor.getValue();

    assertThat(savedTransaction.getAmount()).isEqualByComparingTo(BigDecimal.TEN);
    assertThat(savedTransaction.getType()).isEqualTo(TransactionType.OPENING_BALANCE);
    assertThat(savedTransaction.getAccount()).isEqualTo(account);
    assertThat(savedTransaction.getCategory()).isEqualTo(mockCategory);
  }

}
