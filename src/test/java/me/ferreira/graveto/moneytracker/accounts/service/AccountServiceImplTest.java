package me.ferreira.graveto.moneytracker.accounts.service;

import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.accounts.service.command.CreateAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.impl.AccountServiceImpl;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AccountServiceImplTest {

    @InjectMocks
    private AccountServiceImpl service;
    @Mock
    private TransactionService transactionService;
    @Mock
    private AccountRepository accountRepository;

    @Test
    void shouldCreateNewAccount() {
        // Arrange
        final BigDecimal expectedBalance = BigDecimal.TEN;
        final String expectedInstitution = "Santander";
        final CreateAccountCommand command = new CreateAccountCommand(
                UUID.randomUUID(),
                Currency.EUR,
                expectedBalance,
                expectedInstitution
        );

        when(accountRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        final Transaction mockTransaction = new Transaction();
        when(transactionService.createOpeningBalance(any(), any())).thenReturn(mockTransaction);

        // Act
        final Account createdAccount = service.createAccount(command);

        // Assert
        final ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());

        final Account savedAccount = captor.getValue();

        assertThat(createdAccount.getInstitution()).isEqualTo(expectedInstitution);
        assertThat(createdAccount.getBaseCurrency()).isEqualTo(Currency.EUR);
        assertThat(createdAccount.getSid()).isEqualTo(savedAccount.getSid());
        assertThat(createdAccount.getBalance()).isEqualByComparingTo(BigDecimal.TEN);
    }

}
