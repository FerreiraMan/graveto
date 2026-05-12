package me.ferreira.graveto.moneytracker.transactions.web.dto.request;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

public record UpdateTransactionRequestDto(
    TransactionType transactionType,
    UUID categorySid,

    @Positive(message = "Amount must be a positive value.")
    BigDecimal amount,

    String description,

    @PastOrPresent(message = "Only past or present transactions allowed.")
    LocalDateTime occurredAt
) {
}
