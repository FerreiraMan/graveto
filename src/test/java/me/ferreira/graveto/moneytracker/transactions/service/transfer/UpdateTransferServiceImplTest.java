package me.ferreira.graveto.moneytracker.transactions.service.transfer;

import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.UpdateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.transfer.TransferServiceImpl;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import me.ferreira.graveto.moneytracker.utils.CategoryUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateTransferServiceImplTest {

    @InjectMocks
    private TransferServiceImpl service;
    @Mock
    private CategoryService categoryService;
    @Mock
    private AccountService accountService;
    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void shouldThrowIfTransferTransactionsAreNotFoundDuringTransferUpdate() {
        // Arrange
        final UUID correlationId = UUID.randomUUID();
        when(transactionRepository.findAllByCorrelationId(correlationId)).thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> {
            service.updateTransfer(new UpdateTransferCommand(UUID.randomUUID(), correlationId, null, null, null));
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Transfer is associated with an incorrect amount of transactions.");
    }

    @Test
    void shouldThrowIfTransferTransactionsAreNotOfCorrectType() {
        // Arrange
        final UUID correlationId = UUID.randomUUID();
        final Transaction txOut = new Transaction();
        txOut.setType(TransactionType.TRANSFER_OUT);

        final Transaction txInvalid = new Transaction();
        txInvalid.setType(TransactionType.EXPENSE); // Corrupted!

        when(transactionRepository.findAllByCorrelationId(correlationId)).thenReturn(List.of(txOut, txInvalid));

        // Act & Assert
        assertThatThrownBy(() -> {
            service.updateTransfer(new UpdateTransferCommand(UUID.randomUUID(), correlationId, null, null, null));
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Corrupted transfer does not contain exactly one IN and one OUT transaction.");
    }

    @Test
    void shouldThrowIfUserIsNotAuthorizedToUpdateTransfer() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final UUID correlationId = UUID.randomUUID();

        // Account with NO permissions
        final Account unauthorizedAccount = AccountUtils.createAccount(UUID.randomUUID(), userSid, null);

        final Transaction txOut = new Transaction();
        txOut.setType(TransactionType.TRANSFER_OUT);
        txOut.setAccount(unauthorizedAccount);

        final Transaction txIn = new Transaction();
        txIn.setType(TransactionType.TRANSFER_IN);
        txIn.setAccount(unauthorizedAccount);

        when(transactionRepository.findAllByCorrelationId(correlationId)).thenReturn(List.of(txOut, txIn));

        final UpdateTransferCommand command = new UpdateTransferCommand(userSid, correlationId, BigDecimal.TEN, null, null);

        // Act & Assert
        assertThatThrownBy(() -> {
            service.updateTransfer(command);
        }).isInstanceOf(InsufficientPermissionsException.class)
                .hasMessage("User does not have the required role to update transfer transactions for this account.");
    }

    @ParameterizedTest
    @MethodSource("updateTransferPayload")
    void shouldUpdateTransferTransactionsAndAccountBalancesIfApplicable(
            final BigDecimal persistedAmount,
            final UpdateTransferCommand command,
            final BigDecimal expectedOutBalance,
            final BigDecimal expectedInBalance,
            final BigDecimal expectedAmount) {

        // Arrange
        final UUID userSid = command.userSid();
        final UUID correlationId = command.correlationId();

        // 1. Setup Source Account (Starts with 200, already transferred 'persistedAmount' out)
        final Account outAccount = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
        outAccount.setBalance(new BigDecimal("200.00").subtract(persistedAmount));

        // 2. Setup Destination Account (Starts with 100, already transferred 'persistedAmount' in)
        final Account inAccount = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
        inAccount.setBalance(new BigDecimal("100.00").add(persistedAmount));

        final Category outCategory = CategoryUtils.createCategory("Transfer Out", userSid, null, true);
        final Category inCategory = CategoryUtils.createCategory("Transfer In", userSid, null, true);
        final String persistedDescription = "Initial Transfer";
        final LocalDateTime persistedOccurredAt = LocalDateTime.now().minusDays(2);

        final Transaction txOut = createTransaction(outAccount, outCategory, persistedAmount, persistedDescription, TransactionType.TRANSFER_OUT, persistedOccurredAt);
        final Transaction txIn = createTransaction(inAccount, inCategory, persistedAmount, persistedDescription, TransactionType.TRANSFER_IN, persistedOccurredAt);

        when(transactionRepository.findAllByCorrelationId(correlationId)).thenReturn(List.of(txOut, txIn));

        // Act
        final TransferResult updatedTransfer = service.updateTransfer(command);

        // Assert - Entities
        assertThat(updatedTransfer.expense().getAmount()).isEqualByComparingTo(expectedAmount);
        assertThat(updatedTransfer.income().getAmount()).isEqualByComparingTo(expectedAmount);

        if (command.description() != null) {
            assertThat(updatedTransfer.expense().getDescription()).isEqualTo(command.description());
            assertThat(updatedTransfer.income().getDescription()).isEqualTo(command.description());
        }

        if (command.occurredAt() != null) {
            assertThat(updatedTransfer.expense().getOccurredAt()).isEqualTo(command.occurredAt());
            assertThat(updatedTransfer.income().getOccurredAt()).isEqualTo(command.occurredAt());
        }

        // Assert - Math!
        assertThat(outAccount.getBalance()).isEqualByComparingTo(expectedOutBalance);
        assertThat(inAccount.getBalance()).isEqualByComparingTo(expectedInBalance);
    }

    private Transaction createTransaction(final Account account, final Category category,
                                          final BigDecimal amount, final String description, final TransactionType type, final LocalDateTime time) {
        final Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setType(type);
        transaction.setOccurredAt(time);
        return transaction;
    }

    private static Stream<Arguments> updateTransferPayload() {

        final UUID userSid = UUID.randomUUID();
        final UUID correlationId = UUID.randomUUID();
        final LocalDateTime newDate = LocalDateTime.now();

        return Stream.of(
                // CASE 1: Increase Transfer Amount (50 -> 80)
                // Source Base: 200. Transferred Out 50 = 150.
                // Math: Reverse (-50 out) -> 200. Apply (-80 out) -> 120. Expected Out: 120
                // Dest Base: 100. Transferred In 50 = 150.
                // Math: Reverse (+50 in) -> 100. Apply (+80 in) -> 180. Expected In: 180
                Arguments.of(
                        new BigDecimal("50.00"), // Persisted Amount
                        new UpdateTransferCommand(userSid, correlationId, new BigDecimal("80.00"), null, null),
                        new BigDecimal("120.00"), // Expected Out Balance
                        new BigDecimal("180.00"), // Expected In Balance
                        new BigDecimal("80.00")   // Expected Entity Amount
                ),

                // CASE 2: Decrease Transfer Amount (50 -> 20)
                // Source Base: 200. Transferred Out 50 = 150.
                // Math: Reverse (-50 out) -> 200. Apply (-20 out) -> 180. Expected Out: 180
                // Dest Base: 100. Transferred In 50 = 150.
                // Math: Reverse (+50 in) -> 100. Apply (+20 in) -> 120. Expected In: 120
                Arguments.of(
                        new BigDecimal("50.00"),
                        new UpdateTransferCommand(userSid, correlationId, new BigDecimal("20.00"), "Lowered amount", null),
                        new BigDecimal("180.00"),
                        new BigDecimal("120.00"),
                        new BigDecimal("20.00")
                ),

                // CASE 3: No Math Change (Just update description and date)
                // Source Base: 200. Transferred Out 50 = 150. Expected Out: 150
                // Dest Base: 100. Transferred In 50 = 150. Expected In: 150
                Arguments.of(
                        new BigDecimal("50.00"),
                        new UpdateTransferCommand(userSid, correlationId, null, "Updated Description", newDate),
                        new BigDecimal("150.00"),
                        new BigDecimal("150.00"),
                        new BigDecimal("50.00")
                )
        );
    }

}
