package me.ferreira.graveto.moneytracker.transactions.domain;

import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.utils.CategoryUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionTest {

    @Test
    void shouldCreateOpeningTransactionWithGeneratedSid() {
        // Arrange
        final BigDecimal amount = new BigDecimal("1000.50");
        final Currency currency = Currency.EUR;
        final String institution = "Santander";
        final Account account = Account.create(amount, currency, institution);
        final Category validCategory = CategoryUtils.createInitialBalanceCategory();

        // Act
        final Transaction transaction = Transaction.createOpeningTransaction(account, amount, validCategory);

        // Assert
        assertThat(transaction.getSid()).isNotNull();
        assertThat(transaction.getAccount().getSid()).isEqualTo(account.getSid());
        assertThat(transaction.getAmount()).isEqualTo(account.getBalance());
        assertThat(transaction.getCategory()).isEqualTo(validCategory);
        assertThat(transaction.getCurrency()).isEqualTo(account.getBaseCurrency());
        assertThat(transaction.getType()).isEqualTo(TransactionType.OPENING_BALANCE);
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.ACTIVE);
    }

}
