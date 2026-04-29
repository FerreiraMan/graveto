package me.ferreira.graveto.moneytracker.categories.service.command;

import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

import java.util.UUID;

public record CreateCategoryCommand(
    UUID userSid,
    String name,
    UUID parentSid,
    TransactionType transactionType
) {}
