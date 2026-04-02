package me.ferreira.graveto.moneytracker.transactions.web.dto.response;

import java.util.UUID;

public record TransactionResponseDTO(
    UUID sid,
    String status
) {}
