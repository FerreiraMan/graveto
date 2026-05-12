package me.ferreira.graveto.moneytracker.transactions.web.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

public record TransactionFilterRequestDto(
    @NotNull
    UUID accountSid,
    UUID categorySid,
    LocalDate startDate,
    LocalDate endDate,
    TransactionType type,
    TransactionStatus status
) {
}
