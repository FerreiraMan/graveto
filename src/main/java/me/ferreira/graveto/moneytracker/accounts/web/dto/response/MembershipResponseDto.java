package me.ferreira.graveto.moneytracker.accounts.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.service.payload.AccountDetails;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MembershipResponseDto(
    UUID sid,
    String email,
    String role
) {

  public static MembershipResponseDto from(final AccountMembership membership) {
    return new MembershipResponseDto(
        membership.getUserSid(),
        null,
        membership.getRole().name()
    );
  }

  public static MembershipResponseDto from(final AccountDetails.MembershipDetails user) {
    return new MembershipResponseDto(
        user.sid(),
        user.email(),
        user.role()
    );
  }

}
