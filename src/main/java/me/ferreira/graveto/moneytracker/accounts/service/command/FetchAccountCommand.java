package me.ferreira.graveto.moneytracker.accounts.service.command;

import java.util.UUID;

public record FetchAccountCommand(
    UUID userSid,
    UUID accountSid
) {
}
