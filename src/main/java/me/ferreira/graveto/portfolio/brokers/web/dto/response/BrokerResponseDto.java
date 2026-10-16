package me.ferreira.graveto.portfolio.brokers.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.service.payload.BrokerDetails;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BrokerResponseDto(
    UUID sid,
    String name,
    String status,
    String currency,
    UUID accountSid,
    List<MembershipResponseDto> users
) {

  public static BrokerResponseDto from(final Broker broker) {
    final List<MembershipResponseDto> membershipResponseDto = broker.getMemberships().stream()
        .map(MembershipResponseDto::from)
        .toList();

    return new BrokerResponseDto(
        broker.getSid(),
        broker.getName(),
        broker.getStatus().name(),
        broker.getCurrency().name(),
        broker.getAccountSid(),
        membershipResponseDto
    );
  }

  public static BrokerResponseDto from(final BrokerDetails brokerDetails) {
    final List<MembershipResponseDto> membershipResponseDto = brokerDetails.users().stream()
        .map(MembershipResponseDto::from)
        .toList();

    return new BrokerResponseDto(
        brokerDetails.sid(),
        brokerDetails.name(),
        brokerDetails.status().name(),
        brokerDetails.currency().name(),
        brokerDetails.accountSid(),
        membershipResponseDto
    );
  }

}
