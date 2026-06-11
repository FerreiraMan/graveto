package me.ferreira.graveto.moneytracker.categories.service.command;

import java.util.UUID;

public record FetchAllCategoriesCommand(
    UUID userSid,
    UUID accountSid
) {
}
