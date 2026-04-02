package me.ferreira.graveto.moneytracker.categories.service.command;

import java.util.UUID;

public record FetchCategoryCommand(
    UUID userSid,
    UUID categorySid
) {}
