package me.ferreira.graveto.moneytracker.accounts.service.command;

import java.math.BigDecimal;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;

public record CreateAccountCommand(
    UUID userSid,
    Currency baseCurrency,
    BigDecimal initialBalance,
    String institution
) {
}
