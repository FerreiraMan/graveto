package me.ferreira.graveto.moneytracker.categories.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.categories.domain.Category;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryResponseDto(
    UUID sid,
    String displayName,
    UUID accountSid,
    UUID parentSid,
    String type,
    boolean isSystem
) {

  public static CategoryResponseDto from(final Category category) {
    return new CategoryResponseDto(
        category.getSid(),
        category.getDisplayName(),
        category.getAccountSid(),
        Objects.nonNull(category.getParent()) ? category.getParent().getSid() : null,
        category.getTransactionType().name(),
        Objects.isNull(category.getAccountSid())
    );
  }

}
