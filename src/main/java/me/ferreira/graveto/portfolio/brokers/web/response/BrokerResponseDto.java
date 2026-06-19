package me.ferreira.graveto.portfolio.brokers.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BrokerResponseDto(
    UUID sid,
    String name,
    String status,
    String currency,
    UUID accountSid,
    List<MembershipResponseDto> users
) {

  public record MembershipResponseDto(
      UUID sid,
      String email,
      String role
  ) {
  }

}
