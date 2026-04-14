package me.ferreira.graveto.moneytracker.transactions.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionResponseDTO(
    UUID sid,
    BigDecimal amount,
    String categoryName,
    String description,
    String type,
    String status,
    LocalDateTime occurredAt
) {}
