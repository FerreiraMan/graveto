package me.ferreira.graveto.moneytracker.categories.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

public record CreateCategoryRequestDto(
    @NotBlank(message = "Category name cannot be empty")
    String name,

    UUID parentSid,

    @NotNull(message = "Transaction type is required.")
    TransactionType transactionType
) {
}
