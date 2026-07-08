package me.ferreira.graveto.portfolio.brokers.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BrokerSummaryResponseDto(
    UUID sid,
    String name,
    String baseCurrency,
    UUID accountSid,
    String status
) {
}
