package me.ferreira.graveto.moneytracker.accounts.domain.event;

import me.ferreira.graveto.moneytracker.accounts.domain.Account;

import java.math.BigDecimal;

public record AccountCreatedEvent(
    Account account,
    BigDecimal amount
) {}
