package me.ferreira.graveto.portfolio.brokers.service.payload;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerStatus;

public record BrokerDetails(
    UUID sid,
    String name,
    BrokerStatus status,
    Currency currency,
    UUID accountSid,
    List<BrokerDetails.MembershipDetails> users
) {
  public record MembershipDetails(
      UUID sid,
      String email,
      String role
  ) {
  }
}
