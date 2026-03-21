package me.ferreira.graveto.moneytracker.accounts.service;

import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.accounts.service.command.CreateAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.command.FetchAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.impl.AccountServiceImpl;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        final UUID userSid = UUID.randomUUID();
        final CreateAccountCommand command = new CreateAccountCommand(
                userSid,
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
        assertThat(createdAccount.getMemberships().size()).isEqualTo(1);
        assertThat(createdAccount.getMemberships().getFirst().getUserSid()).isEqualTo(userSid);
        assertThat(createdAccount.getMemberships().getFirst().getRole()).isEqualTo(MembershipRole.OWNER);
        assertThat(createdAccount.getMemberships().getFirst().getAccount()).isEqualTo(savedAccount);
    }

    @Test
    void shouldReturnAccount() {
        // Arrange
        final UUID accountSid = UUID.randomUUID();
        final UUID userSid = UUID.randomUUID();
        final Account expectedAccount = AccountUtils.createAccount(accountSid, userSid);
        final FetchAccountCommand command = new FetchAccountCommand(accountSid, userSid);

        when(accountRepository.findBySidAndUserSid(userSid, accountSid)).thenReturn(Optional.of(expectedAccount));

        // Act
        final Account fetchedAccount = service.fetchAccount(command);

        //Assert
        assertThat(fetchedAccount.getSid()).isEqualTo(expectedAccount.getSid());
        assertThat(fetchedAccount.getMemberships().getFirst().getUserSid()).isEqualTo(expectedAccount.getMemberships().getFirst().getUserSid());
    }

    @Test
    void shouldThrowWhenAccountDoesNotExistOrIsNotAssociatedWithAccount() {
        // Arrange
        final FetchAccountCommand command = new FetchAccountCommand(
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        when(accountRepository.findBySidAndUserSid(any(), any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> {
            service.fetchAccount(command);
        }).isInstanceOf(AccountNotFoundException.class);
    }

}
