package me.ferreira.graveto.moneytracker.transactions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.FindAllTransactionsCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    final UUID accountSid = UUID.randomUUID();

    when(accountService.fetchAccountEntity(any())).thenThrow(new AccountNotFoundException(accountSid));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.findAll(mock(FindAllTransactionsCommand.class));
    }).isInstanceOf(AccountNotFoundException.class)
        .hasMessage("Account with SID [" + accountSid + "] was not found or you do not have permission to view it.");
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

    when(accountService.fetchAccountEntity(any())).thenReturn(mock(Account.class));
    final Page<Transaction> expectedPage = mock(Page.class);
    when(transactionRepository.findAll(command)).thenReturn(expectedPage);

    // Act
    final Page<Transaction> actualPage = service.findAll(command);

    // Assert
    assertThat(actualPage).isSameAs(expectedPage);

    final ArgumentCaptor<UUID> fetchAccountCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(accountService).fetchAccountEntity(fetchAccountCaptor.capture());

    final UUID passedFetchArgument = fetchAccountCaptor.getValue();
    assertThat(passedFetchArgument).isEqualTo(accountSid);

    verify(transactionRepository).findAll(command);
  }

}
