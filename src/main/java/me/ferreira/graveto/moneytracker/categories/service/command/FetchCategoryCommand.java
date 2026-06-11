package me.ferreira.graveto.moneytracker.categories.service.command;

import java.util.UUID;

public record FetchCategoryCommand(
    UUID accountSid,
    UUID categorySid
) {
}
