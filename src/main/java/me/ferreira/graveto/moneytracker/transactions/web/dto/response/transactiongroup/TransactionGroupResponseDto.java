package me.ferreira.graveto.moneytracker.transactions.web.dto.response.transactiongroup;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.payload.AssignToGroupDetails;
import me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.payload.UngroupDetails;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionGroupResponseDto(
    UUID groupSid,
    String groupName,
    Long totalTransactions,
    Integer added,
    Integer moved,
    Integer alreadyInGroup,
    Integer removed,
    Integer alreadyUngrouped
) {

  public static TransactionGroupResponseDto from(final AssignToGroupDetails assignToGroupDetails) {
    return new TransactionGroupResponseDto(
        assignToGroupDetails.groupSid(),
        assignToGroupDetails.groupName(),
        assignToGroupDetails.totalTransactions(),
        assignToGroupDetails.added(),
        assignToGroupDetails.moved(),
        assignToGroupDetails.alreadyInGroup(),
        null,
        null
    );
  }

  public static TransactionGroupResponseDto from(final UngroupDetails ungroupDetails) {
    return new TransactionGroupResponseDto(
        null,
        null,
        null,
        null,
        null,
        null,
        ungroupDetails.removed(),
        ungroupDetails.alreadyUngrouped()
    );
  }

}