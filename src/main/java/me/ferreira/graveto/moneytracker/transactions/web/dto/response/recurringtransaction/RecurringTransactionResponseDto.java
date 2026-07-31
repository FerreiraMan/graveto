package me.ferreira.graveto.moneytracker.transactions.web.dto.response.recurringtransaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;

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

  public static RecurringTransactionResponseDto from(final RecurringTransaction recurringTransaction) {

    return new RecurringTransactionResponseDto(
        recurringTransaction.getSid(),
        new RecurringTransactionResponseDto.EnhancedInfoObject(recurringTransaction.getAccount().getSid(),
            recurringTransaction.getAccount().getInstitution()),
        new RecurringTransactionResponseDto.EnhancedInfoObject(recurringTransaction.getCategory().getSid(),
            recurringTransaction.getCategory().getDisplayName()),
        recurringTransaction.getUserSid(),
        recurringTransaction.getDescription(),
        recurringTransaction.getAmount(),
        recurringTransaction.getCurrency().name(),
        recurringTransaction.getType().name(),
        recurringTransaction.getFrequency().name(),
        recurringTransaction.getNextExecutionDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
        recurringTransaction.getStatus().name(),
        recurringTransaction.getEndDate() == null ? null :
            recurringTransaction.getEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
    );
  }

}