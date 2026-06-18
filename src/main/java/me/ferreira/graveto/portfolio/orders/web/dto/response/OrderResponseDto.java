package me.ferreira.graveto.portfolio.orders.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResponseDto(
    UUID sid,
    EnhancedInfoObject broker,
    EnhancedInfoObject asset,
    String orderType,
    BigDecimal quantity,
    BigDecimal pricePerUnit,
    BigDecimal fees,
    String currency,
    LocalDateTime executedAt,
    String notes
) {
  public record EnhancedInfoObject(
      UUID sid,
      String name
  ) {
  }
}