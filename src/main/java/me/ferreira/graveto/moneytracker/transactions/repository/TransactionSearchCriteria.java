package me.ferreira.graveto.moneytracker.transactions.repository;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.service.command.FindAllTransactionsCommand;
import org.springframework.data.domain.Pageable;

public record TransactionSearchCriteria(
    UUID accountSid,
    Set<Long> categoryIds,
    LocalDate startDate,
    LocalDate endDate,
    TransactionType type,
    TransactionStatus status,
    Pageable pageable
) {

  public static TransactionSearchCriteria from(final FindAllTransactionsCommand command, final Set<Long> categoryIds) {
    return new TransactionSearchCriteria(
        command.accountSid(),
        categoryIds,
        command.startDate(),
        command.endDate(),
        command.type(),
        command.status(),
        command.pageable()
    );
  }

}
