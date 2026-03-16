package me.ferreira.graveto.moneytracker.accounts.service.command;

import me.ferreira.graveto.common.domain.Currency;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountCommand(
        UUID userSid,
        Currency baseCurrency,
        BigDecimal initialBalance,
        String institution
) {}
