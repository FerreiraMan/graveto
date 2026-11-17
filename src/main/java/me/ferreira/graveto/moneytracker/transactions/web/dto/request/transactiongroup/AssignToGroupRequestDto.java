package me.ferreira.graveto.moneytracker.transactions.web.dto.request.transactiongroup;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

public record AssignToGroupRequestDto(
    @NotNull(message = "Account is required.")
    UUID accountSid,

    UUID groupSid,

    @Size(max = 255, message = "Group name cannot exceed 255 characters.")
    String groupName,

    @NotEmpty(message = "Transaction list cannot be empty.")
    @Size(max = 500, message = "A maximum of 500 transactions can be assigned at once.")
    Set<UUID> transactionSids,

    Boolean replaceExistingGroup
) {

  public AssignToGroupRequestDto {
    if (replaceExistingGroup == null) {
      replaceExistingGroup = false;
    }
  }

  @AssertTrue(message = "Either a group or a group name must be provided.")
  public boolean isGroupTargetProvided() {
    return groupSid != null || StringUtils.isNotBlank(groupName);
  }

}
