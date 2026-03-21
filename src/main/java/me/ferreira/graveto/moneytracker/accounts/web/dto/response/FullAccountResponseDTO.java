package me.ferreira.graveto.moneytracker.accounts.web.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FullAccountResponseDTO(
    UUID sid,
    BigDecimal balance,
    String baseCurrency,
    String status,
    String institution,
    List<MembershipResponseDTO> users
) {}
