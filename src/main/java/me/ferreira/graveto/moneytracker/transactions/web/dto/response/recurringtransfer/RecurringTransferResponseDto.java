package me.ferreira.graveto.moneytracker.transactions.web.dto.response.recurringtransfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;

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

  public static RecurringTransferResponseDto from(final RecurringTransfer recurringTransfer) {

    return new RecurringTransferResponseDto(
        recurringTransfer.getSid(),
        new RecurringTransferResponseDto.EnhancedInfoObject(recurringTransfer.getSourceAccount().getSid(),
            recurringTransfer.getSourceAccount().getInstitution()),
        new RecurringTransferResponseDto.EnhancedInfoObject(recurringTransfer.getDestinationAccount().getSid(),
            recurringTransfer.getDestinationAccount().getInstitution()),
        recurringTransfer.getUserSid(),
        recurringTransfer.getDescription(),
        recurringTransfer.getAmount(),
        recurringTransfer.getCurrency().name(),
        recurringTransfer.getFrequency().name(),
        recurringTransfer.getNextExecutionDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
        recurringTransfer.getStatus().name(),
        recurringTransfer.getEndDate() == null ? null :
            recurringTransfer.getEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
    );
  }

}
