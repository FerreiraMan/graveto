package me.ferreira.graveto.moneytracker.transactions.web.dto.request.transfer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateTransferRequestDTO(
        @NotNull(message = "Source Account identification is required.")
        UUID sourceAccountSid,

        @NotNull(message = "Destination Account identification is required.")
        UUID destinationAccountSid,

        @NotNull(message = "Amount is required.")
        @Positive(message = "Amount must be a positive value.")
        BigDecimal amount,

        String description,

        @PastOrPresent(message = "Only past or present transactions allowed.")
        LocalDateTime occurredAt
) {}
