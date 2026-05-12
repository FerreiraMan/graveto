package me.ferreira.graveto.moneytracker.accounts.web.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountSummaryResponseDto(
    UUID sid,
    String institution,
    BigDecimal balance,
    String baseCurrency,
    String status
) {
}
