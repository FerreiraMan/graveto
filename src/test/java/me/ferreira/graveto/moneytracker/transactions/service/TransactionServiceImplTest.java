package me.ferreira.graveto.moneytracker.transactions.service;

import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.CategoryNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.CreateTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.TransactionServiceImpl;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import me.ferreira.graveto.moneytracker.utils.CategoryUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplTest {

    @InjectMocks
    private TransactionServiceImpl service;
    @Mock
    private CategoryService categoryService;
    @Mock
    private AccountService accountService;
    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void shouldThrowIfCategoryIsNotFoundDuringTransactionCreation() {
        // Arrange
        final UUID categorySid = UUID.randomUUID();

        when(categoryService.fetchCategory(any())).thenThrow(new CategoryNotFoundException(categorySid));

        // Act & Assert
        assertThatThrownBy(() -> {
            service.createTransaction(Mockito.mock(CreateTransactionCommand.class));
        }).isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category with SID ["+ categorySid + "] was not found or does not belong to the user.");
    }

    @Test
    void shouldThrowIfAccountIsNotFoundDuringTransactionCreation() {
        // Arrange
        final UUID categorySid = UUID.randomUUID();

        when(categoryService.fetchCategory(any())).thenReturn(Mockito.mock(Category.class));
        when(accountService.fetchAccount(any())).thenThrow(new AccountNotFoundException(categorySid));

        // Act & Assert
        assertThatThrownBy(() -> {
            service.createTransaction(Mockito.mock(CreateTransactionCommand.class));
        }).isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account with SID ["+ categorySid + "] was not found or you do not have permission to view it.");
    }

    @Test
    void shouldThrowIfUserIsNotAuthorizedToCreateTransaction() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, null);
        final CreateTransactionCommand command = mock(CreateTransactionCommand.class);

        when(categoryService.fetchCategory(any())).thenReturn(Mockito.mock(Category.class));
        when(accountService.fetchAccount(any())).thenReturn(account);
        when(command.userSid()).thenReturn(userSid);

        // Act & Assert
        assertThatThrownBy(() -> {
            service.createTransaction(command);
        }).isInstanceOf(InsufficientPermissionsException.class)
                .hasMessage("User does not have the required role to create transactions for this account.");
    }

    @Test
    void shouldCreateExpenseTransaction() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final BigDecimal transactionAmount = BigDecimal.TEN;
        final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
        final BigDecimal initialBalance = account.getBalance();

        // Act
        final BigDecimal resultingAccountBalance = createAndAssertTransaction(account, transactionAmount, TransactionType.EXPENSE);

        // Assert
        assertThat(resultingAccountBalance).isEqualByComparingTo(initialBalance.subtract(transactionAmount));
    }

    @Test
    void shouldCreateIncomeTransaction() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final BigDecimal transactionAmount = BigDecimal.TEN;
        final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
        final BigDecimal initialBalance = account.getBalance();

        // Act
        final BigDecimal resultingAccountBalance = createAndAssertTransaction(account, transactionAmount, TransactionType.INCOME);

        // Assert
        assertThat(resultingAccountBalance).isEqualByComparingTo(initialBalance.add(transactionAmount));
    }

    private BigDecimal createAndAssertTransaction(final Account account,
                                                  final BigDecimal transactionAmount,
                                                  final TransactionType transactionType) {
        // Arrange
        final UUID userSid = account.getMemberships().getFirst().getUserSid();
        final String categoryName = "Restaurants";
        final Category category = CategoryUtils.createCategory(categoryName, null, null, false);
        final LocalDateTime occurredAt = LocalDateTime.now().minusDays(1);

        final String description = "Lunch";

        final CreateTransactionCommand command = new CreateTransactionCommand(
                userSid, account.getSid(), category.getSid(), transactionAmount, description, transactionType, occurredAt
        );

        when(categoryService.fetchCategory(any())).thenReturn(category);
        when(accountService.fetchAccount(any())).thenReturn(account);
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // Act
        service.createTransaction(command);

        // Assert
        final ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());

        final Transaction savedTransaction = captor.getValue();

        assertThat(savedTransaction.getSid()).isNotNull();
        assertThat(savedTransaction.getAmount()).isEqualByComparingTo(transactionAmount);
        assertThat(savedTransaction.getDescription()).isEqualTo(description);
        assertThat(savedTransaction.getType()).isEqualTo(transactionType);
        assertThat(savedTransaction.getCurrency()).isEqualTo(account.getBaseCurrency());
        assertThat(savedTransaction.getStatus()).isEqualTo(TransactionStatus.ACTIVE);
        assertThat(savedTransaction.getCategory()).isEqualTo(category);
        assertThat(savedTransaction.getAccount()).isEqualTo(account);
        assertThat(savedTransaction.getOccurredAt()).isEqualTo(occurredAt);

        return account.getBalance();
    }

}
