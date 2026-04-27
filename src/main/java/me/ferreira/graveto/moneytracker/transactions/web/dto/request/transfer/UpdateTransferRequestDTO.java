package me.ferreira.graveto.moneytracker.transactions.web.dto.request.transfer;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateTransferRequestDTO(
    @Positive(message = "Amount must be a positive value.")
    BigDecimal amount,

    String description,

    @PastOrPresent(message = "Only past or present transactions allowed.")
    LocalDateTime occurredAt
) {}
