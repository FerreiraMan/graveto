package me.ferreira.graveto.moneytracker.transactions.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionResponseDto(
    UUID sid,
    BigDecimal amount,
    String currency,
    String description,
    String type,
    UUID correlationId,
    EnhancedInfoObject account,
    EnhancedInfoObject category,
    String status,
    LocalDateTime deletedAt,
    LocalDateTime occurredAt
) {

  public record EnhancedInfoObject(
      UUID sid,
      String name
  ) {
  }

  public static TransactionResponseDto from(final Transaction transaction) {
    return new TransactionResponseDto(
        transaction.getSid(),
        transaction.getAmount(),
        transaction.getCurrency().name(),
        transaction.getDescription() != null ? transaction.getDescription() : null,
        transaction.getType().name(),
        transaction.getCorrelationId() != null ? transaction.getCorrelationId() : null,
        new TransactionResponseDto.EnhancedInfoObject(transaction.getAccount().getSid(),
            transaction.getAccount().getInstitution()),
        new TransactionResponseDto.EnhancedInfoObject(transaction.getCategory().getSid(),
            transaction.getCategory().getDisplayName()),
        transaction.getStatus().name(),
        transaction.getDeletedAt() != null ? transaction.getDeletedAt() : null,
        transaction.getOccurredAt()
    );
  }

}
