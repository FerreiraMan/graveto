package me.ferreira.graveto.moneytracker.accounts.domain;

import me.ferreira.graveto.common.domain.Currency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountTest {

    @Test
    void shouldCreateActiveAccountWithGeneratedSid() {
        // Arrange
        final BigDecimal initialBalance = new BigDecimal("1000.50");
        final Currency currency = Currency.EUR;
        final String institution = "Santander";

        // Act
        final Account account = Account.create(initialBalance, currency, institution);

        // Assert
        assertThat(account.getSid()).isNotNull();
        assertThat(account.getBalance()).isEqualTo(initialBalance);
        assertThat(account.getBaseCurrency()).isEqualTo(Currency.EUR);
        assertThat(account.getInstitution()).isEqualTo("Santander");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

}
