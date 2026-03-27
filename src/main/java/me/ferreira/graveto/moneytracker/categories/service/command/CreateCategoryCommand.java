package me.ferreira.graveto.moneytracker.categories.service.command;

import java.util.UUID;

public record CreateCategoryCommand(
    UUID userSid,
    String name,
    UUID parentSid
) {}
