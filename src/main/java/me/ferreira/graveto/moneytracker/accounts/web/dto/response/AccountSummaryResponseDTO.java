package me.ferreira.graveto.moneytracker.accounts.web.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountSummaryResponseDTO(
    UUID sid,
    String institution,
    BigDecimal balance,
    String baseCurrency,
    String status
) {}
