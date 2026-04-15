package me.ferreira.graveto.moneytracker.transactions.service.command;

import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateTransactionCommand(
   UUID userSid,
   UUID transactionSid,
   TransactionType transactionType,
   UUID categorySid,
   BigDecimal amount,
   String description
) {}
