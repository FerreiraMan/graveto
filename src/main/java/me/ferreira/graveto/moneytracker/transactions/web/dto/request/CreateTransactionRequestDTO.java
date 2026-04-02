package me.ferreira.graveto.moneytracker.transactions.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateTransactionRequestDTO(
    @NotNull(message = "Account identification is required.")
    UUID accountSid,

    @NotNull(message = "Category identification is required.")
    UUID categorySid,

    @NotNull(message = "Amount is required.")
    @Positive(message = "Amount must be a positive value.")
    BigDecimal amount,

    String description,

    @NotNull(message = "Transaction type is required.")
    TransactionType transactionType,

    @PastOrPresent(message = "Only past or present transactions allowed.")
    LocalDateTime occurredAt
) {}
