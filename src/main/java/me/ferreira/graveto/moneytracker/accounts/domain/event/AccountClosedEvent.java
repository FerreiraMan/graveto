package me.ferreira.graveto.moneytracker.accounts.domain.event;

import me.ferreira.graveto.moneytracker.accounts.domain.Account;

public record AccountClosedEvent(
    Account account
) {
}