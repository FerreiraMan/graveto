package me.ferreira.graveto.moneytracker.categories.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

import java.util.UUID;

public record CreateCategoryRequestDTO(
    @NotBlank(message = "Category name cannot be empty")
    String name,

    UUID parentSid,

    @NotNull(message = "Transaction type is required.")
    TransactionType transactionType
) {}
