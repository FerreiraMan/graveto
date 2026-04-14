package me.ferreira.graveto.moneytracker.transactions.web.dto.request;

import jakarta.validation.constraints.NotNull;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

import java.time.LocalDate;
import java.util.UUID;

public record TransactionFilterRequestDTO(
    @NotNull
    UUID accountSid,
    UUID categorySid,
    LocalDate startDate,
    LocalDate endDate,
    TransactionType type,
    TransactionStatus status
) {}
