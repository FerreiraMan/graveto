package me.ferreira.graveto.moneytracker.transactions.web.dto.response.recurringtransaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecurringTransactionResponseDto(
    UUID sid,
    EnhancedInfoObject account,
    EnhancedInfoObject category,
    UUID userSid,
    String description,
    BigDecimal amount,
    String currency,
    String transactionType,
    String frequency,
    String nextExecutionDate,
    String status,
    String endDate
) {
  public record EnhancedInfoObject(
      UUID sid,
      String name
  ) {
  }
}