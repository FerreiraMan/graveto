package me.ferreira.graveto.moneytracker.transactions.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponseDTO(
    UUID sid,
    BigDecimal amount,
    String categoryName,
    String description,
    String type,
    String status,
    LocalDateTime occurredAt
) {}
