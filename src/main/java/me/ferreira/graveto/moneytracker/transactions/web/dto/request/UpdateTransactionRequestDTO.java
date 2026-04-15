package me.ferreira.graveto.moneytracker.transactions.web.dto.request;

import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateTransactionRequestDTO(
    TransactionType transactionType,
    UUID categorySid,
    BigDecimal amount,
    String description
) {}
