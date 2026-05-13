package me.ferreira.graveto.moneytracker.accounts.service.command;

import java.util.UUID;

public record CloseAccountCommand(
    UUID userSid,
    UUID accountSid
) {
}
