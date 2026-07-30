package me.ferreira.graveto.moneytracker.transactions.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
}
