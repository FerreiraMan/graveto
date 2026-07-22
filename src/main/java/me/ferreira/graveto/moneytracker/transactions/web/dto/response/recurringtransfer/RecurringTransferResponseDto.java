package me.ferreira.graveto.moneytracker.transactions.web.dto.response.recurringtransfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecurringTransferResponseDto(
    UUID sid,
    RecurringTransferResponseDto.EnhancedInfoObject sourceAccount,
    RecurringTransferResponseDto.EnhancedInfoObject destinationAccount,
    UUID userSid,
    String description,
    BigDecimal amount,
    String currency,
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
