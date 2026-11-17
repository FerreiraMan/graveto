package me.ferreira.graveto.moneytracker.transactions.web.dto.request.transactiongroup;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record UngroupRequestDto(
    @NotNull(message = "Account is required.")
    UUID accountSid,

    @NotEmpty(message = "Transaction list cannot be empty.")
    @Size(max = 500, message = "A maximum of 500 transactions can be ungrouped at once.")
    Set<UUID> transactionSids
) {
}
