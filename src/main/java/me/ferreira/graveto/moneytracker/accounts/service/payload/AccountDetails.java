package me.ferreira.graveto.moneytracker.accounts.service.payload;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;

public record AccountDetails(
    UUID sid,
    BigDecimal balance,
    Currency currency,
    AccountStatus status,
    String institution,
    List<MembershipDetails> users
) {
  public record MembershipDetails(
      UUID sid,
      String email,
      String role
  ) {
  }
}