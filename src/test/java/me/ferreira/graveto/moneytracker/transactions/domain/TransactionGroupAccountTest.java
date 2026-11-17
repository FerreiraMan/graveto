package me.ferreira.graveto.moneytracker.transactions.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import org.junit.jupiter.api.Test;

public class TransactionGroupAccountTest {

  @Test
  void shouldLinkTransactionGroupToAccount() {
    // Arrange
    final TransactionGroup group = TransactionGroup.create("Algarve 2026", UUID.randomUUID());
    final Account account = Account.create(new BigDecimal("1000.50"), Currency.EUR, "Santander");

    // Act
    final TransactionGroupAccount link = TransactionGroupAccount.create(group, account);

    // Assert
    assertThat(link.getTransactionGroup()).isSameAs(group);
    assertThat(link.getAccount()).isSameAs(account);
  }

}
