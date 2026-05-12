package me.ferreira.graveto.moneytracker.accounts.domain.event;

import java.math.BigDecimal;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;

public record AccountCreatedEvent(
    Account account,
    BigDecimal amount
) {
}
