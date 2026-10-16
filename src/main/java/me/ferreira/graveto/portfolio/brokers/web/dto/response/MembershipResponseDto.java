package me.ferreira.graveto.portfolio.brokers.web.dto.response;

import java.util.UUID;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembership;
import me.ferreira.graveto.portfolio.brokers.service.payload.BrokerDetails;

public record MembershipResponseDto(
    UUID sid,
    String email,
    String role
) {

  public static MembershipResponseDto from(final BrokerMembership membership) {
    return new MembershipResponseDto(
        membership.getUserSid(),
        null,
        membership.getRole().name()
    );
  }

  public static MembershipResponseDto from(final BrokerDetails.MembershipDetails user) {
    return new MembershipResponseDto(
        user.sid(),
        user.email(),
        user.role()
    );
  }

}
