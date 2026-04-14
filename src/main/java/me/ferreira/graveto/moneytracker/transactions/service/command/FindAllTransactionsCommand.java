package me.ferreira.graveto.moneytracker.transactions.service.command;

import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public record FindAllTransactionsCommand(
   UUID userSid,
   UUID accountSid,
   UUID categorySid,
   LocalDate startDate,
   LocalDate endDate,
   TransactionType type,
   TransactionStatus status,
   Pageable pageable
) {}
